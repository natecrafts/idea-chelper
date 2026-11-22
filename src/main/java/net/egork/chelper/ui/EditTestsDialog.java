package net.egork.chelper.ui;

import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.colors.EditorFontType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.ui.CheckBoxList;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.components.JBCheckBox;
import com.intellij.ui.components.JBLabel;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import net.egork.chelper.task.Test;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Egor Kulikov (kulikov@devexperts.com)
 */
public class EditTestsDialog extends DialogWrapper {
    private static String TITLE = "Tests";

    private List<Test> tests;
    private int currentTest;
    private CheckBoxList<Test> testList;
    private JBTextArea input;
    private JBTextArea output;
    private JBCheckBox knowAnswer;
    private JPanel outputPanel;
    private boolean updating = false;

    public EditTestsDialog(Test[] tests, Project project) {
        // Set canBeParent = true to allow this dialog to be a parent of other model dialogs.
        super(project, true);
        this.tests = new ArrayList<>(List.of(tests));
        setTitle(TITLE);
        setOKButtonText("Save");
        init(); // This calls createCenterPanel()
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel selectorAndButtonsPanel = new JPanel(new BorderLayout());
        selectorAndButtonsPanel.add(new JBLabel("Tests:"), BorderLayout.NORTH);
        // Update a test's active flag whenever its checkbox is toggled.
        testList = new CheckBoxList<>((index, value) -> {
            if (!updating && index >= 0 && index < tests.size()) {
                tests.set(index, tests.get(index).setActive(value));
            }
        });
        testList.getEmptyText().setText("No tests");
        testList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        testList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (updating) {
                    return;
                }
                int index = testList.getSelectedIndex();
                if (index >= 0 && index < tests.size()) {
                    saveCurrentTest();
                    setSelectedTest(index);
                }
            }
        });
//		testList.addMouseListener(new MouseAdapter() {
//			@Override
//			public void mouseClicked(MouseEvent e) {
//				int index = testList.locationToIndex(e.getPoint());
//				if (index >= 0 && index < testList.getItemsCount()) {
//					saveCurrentTest();
//					setSelectedTest(index);
//				}
//			}
//		});
        selectorAndButtonsPanel.add(new JBScrollPane(testList,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        // The 2-row container for the test-list action buttons.
        // Row 1: All/None, row 2: New/Remove.
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 1));
        JPanel upperButtonsPanel = new JPanel(new GridLayout(1, 2));
        JButton all = new JButton("All");
        all.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < tests.size(); i++) {
                    tests.set(i, tests.get(i).setActive(true));
                }
                setSelectedTest(currentTest);
            }
        });
        upperButtonsPanel.add(all);
        JButton none = new JButton("None");
        none.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < tests.size(); i++) {
                    tests.set(i, tests.get(i).setActive(false));
                }
                setSelectedTest(currentTest);
            }
        });
        upperButtonsPanel.add(none);
        buttonsPanel.add(upperButtonsPanel);
        JPanel middleButtonsPanel = new JPanel(new GridLayout(1, 2));
        JButton newTest = new JButton("New");
        newTest.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveCurrentTest();
                int index = EditTestsDialog.this.tests.size();
                Test test = new Test("", "", index);
                EditTestsDialog.this.tests.add(test);
                setSelectedTest(index);
            }
        });
        middleButtonsPanel.add(newTest);
        JButton remove = new JButton("Remove");
        remove.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (currentTest == -1) {
                    return;
                }
                EditTestsDialog.this.tests.remove(currentTest);
                int size = EditTestsDialog.this.tests.size();
                for (int i = currentTest; i < size; i++) {
                    EditTestsDialog.this.tests.set(i, EditTestsDialog.this.tests.get(i).setIndex(i));
                }
                if (currentTest < size) {
                    setSelectedTest(currentTest);
                    return;
                }
                if (size > 0) {
                    setSelectedTest(0);
                    return;
                }
                setSelectedTest(-1);
            }
        });
        middleButtonsPanel.add(remove);
        buttonsPanel.add(middleButtonsPanel);
        selectorAndButtonsPanel.add(buttonsPanel, BorderLayout.SOUTH);

        // Input and Output text areas.
        // Respect the user's configured Editor font face, font size, and zoom
        Font editorFont = EditorColorsManager.getInstance().getGlobalScheme()
                .getFont(EditorFontType.PLAIN);

        DocumentListener listener = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                saveCurrentTest();
            }

            public void removeUpdate(DocumentEvent e) {
                saveCurrentTest();
            }

            public void changedUpdate(DocumentEvent e) {
                saveCurrentTest();
            }
        };

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(new JBLabel("Input:"), BorderLayout.NORTH);
        input = new JBTextArea();
        input.setFont(editorFont);
        input.getDocument().addDocumentListener(listener);
        inputPanel.add(new JBScrollPane(input), BorderLayout.CENTER);

        outputPanel = new JPanel(new BorderLayout());
        outputPanel.add(new JBLabel("Output:"), BorderLayout.NORTH);
        output = new JBTextArea();
        output.setFont(editorFont);
        output.getDocument().addDocumentListener(listener);
        outputPanel.add(new JBScrollPane(output), BorderLayout.CENTER);
        knowAnswer = new JBCheckBox("Know answer?");
        knowAnswer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveCurrentTest();
            }
        });
        JPanel outputAndCheckBoxPanel = new JPanel(new BorderLayout());
        outputAndCheckBoxPanel.add(knowAnswer, BorderLayout.NORTH);
        outputAndCheckBoxPanel.add(outputPanel, BorderLayout.CENTER);

        // Vertical splitter.
        // - Vertical: true.
        // - Default vertical ratio: 50% top (input), 50% bottom (output).
        JBSplitter testSplitter = new JBSplitter(true, 0.5f);
        // Set a key for IntelliJ to remember last user's split preference.
        testSplitter.setSplitterProportionKey("net.egork.chelper.ui.EditTestsDialog.testSplitter");
        testSplitter.setFirstComponent(inputPanel);
        testSplitter.setSecondComponent(outputAndCheckBoxPanel);

        // Horizontal splitter.
        // - Vertical: false
        // - Default split ratio: 35% left (test list), 65% right (input/output).
        JBSplitter mainSplitter = new JBSplitter(false, 0.35f);
        // Set a key for IntelliJ to remember last user's split preference.
        mainSplitter.setSplitterProportionKey("net.egork.chelper.ui.EditTestsDialog.mainSplitter");
        mainSplitter.setFirstComponent(selectorAndButtonsPanel);
        mainSplitter.setSecondComponent(testSplitter);

        // Select first test if any exists, otherwise -1.
        setSelectedTest(this.tests.isEmpty() ? -1 : 0);
        // HiDPI-scaled default size, but user can resize freely.
        mainSplitter.setPreferredSize(JBUI.size(700, 450));
        return mainSplitter;
    }

    @Override
    protected void doOKAction() {
        saveCurrentTest();
        super.doOKAction();
    }

    /**
     * Overriding this method to return a unique key so that IntelliJ can remember the
     * user's dialog dimensions and positions across sessions.
     */
    @NonNls
    @Override
    protected @Nullable String getDimensionServiceKey() {
        return "net.egork.chelper.ui.EditTestsDialog";
    }

    /**
     * Select a test given the index, or -1 when there are no tests.
     * This method does a few things:
     * <ul>
     *   <li>Set updating = true so that testList's ListSelectionListener ignores
     *   selection changes made by the plugin (rather than changes made by the
     *   user).</li>
     *   <li>Update the Input and Output text boxes and the "Known answer?" check box
     *   on the right.</li>
     *   <li> Refresh the UI of the test list on the left.</li>
     * </ul>
     * @param index the index of the selected test.
     */
    private void setSelectedTest(int index) {
        updating = true;
        currentTest = -1;
        if (index == -1) {
            input.setVisible(false);
            output.setVisible(false);
        } else {
            input.setVisible(true);
            output.setVisible(true);
            input.setText(tests.get(index).input);
            knowAnswer.setSelected(tests.get(index).output != null);
            output.setText(knowAnswer.isSelected() ? tests.get(index).output : "");
            outputPanel.setVisible(knowAnswer.isSelected());
        }
        currentTest = index;
        testList.clear();
        for (Test test : tests) {
            testList.addItem(test, test.toString(), test.active);
        }
        if (currentTest >= 0 && currentTest < tests.size()) {
            testList.setSelectedIndex(currentTest);
        }
        updating = false;
    }

    private void saveCurrentTest() {
        if (currentTest == -1) {
            return;
        }
        tests.set(currentTest, new Test(input.getText(), knowAnswer.isSelected() ? output.getText() : null, currentTest,
                tests.get(currentTest).active));
        outputPanel.setVisible(knowAnswer.isSelected());
//        output.invalidate();
//        output.repaint();
    }

    public static Test[] editTests(Test[] tests, Project project) {
        EditTestsDialog dialog = new EditTestsDialog(tests, project);
        // showAndGet() returns true only if "Save" was clicked.
        if (dialog.showAndGet()) {
            return dialog.tests.toArray(new Test[0]);
        }
        // In other cases (Cancel/Esc/Close window), return original `tests`.
        return tests;
    }
}

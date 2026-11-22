package net.egork.chelper.ui;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.VerticalFlowLayout;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
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
    private static int HEIGHT = new JLabel("Test").getPreferredSize().height;
    private static String TITLE = "Tests";

    private List<Test> tests;
    private int currentTest;
    private JBList testList;
    private JTextArea input;
    private JTextArea output;
    private List<JCheckBox> checkBoxes = new ArrayList<JCheckBox>();
    private JPanel checkBoxesPanel;
    private JCheckBox knowAnswer;
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
        VariableGridLayout mainLayout = new VariableGridLayout(1, 2, 5, 5);
        mainLayout.setColFraction(0, 0.35);
        mainLayout.setColFraction(1, 0.65);
        JPanel mainPanel = new JPanel(mainLayout);
        JPanel selectorAndButtonsPanel = new JPanel(new BorderLayout());
        selectorAndButtonsPanel.add(new JLabel("Tests:"), BorderLayout.NORTH);
        JPanel checkBoxesAndSelectorPanel = new JPanel(new BorderLayout());
        checkBoxesPanel = new JPanel(new VerticalFlowLayout(VerticalFlowLayout.TOP, 0, 0, false, false));
        for (Test test : tests) {
            JCheckBox checkBox = createCheckBox(test);
            checkBoxesPanel.add(checkBox);
        }
        checkBoxesAndSelectorPanel.add(checkBoxesPanel, BorderLayout.WEST);
        testList = new JBList(this.tests.toArray());
        testList.setFixedCellHeight(HEIGHT);
        testList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        testList.setLayoutOrientation(JList.VERTICAL);
        testList.addListSelectionListener(new ListSelectionListener() {
            public void valueChanged(ListSelectionEvent e) {
                if (updating) {
                    return;
                }
                int index = testList.getSelectedIndex();
                if (index >= 0 && index < testList.getItemsCount()) {
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
        checkBoxesAndSelectorPanel.add(testList, BorderLayout.CENTER);
        selectorAndButtonsPanel.add(new JBScrollPane(checkBoxesAndSelectorPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER), BorderLayout.CENTER);
        // The 2-row container for the test-list action buttons.
        // Row 1: All/None, row 2: New/Remove.
        JPanel buttonsPanel = new JPanel(new GridLayout(2, 1));
        JPanel upperButtonsPanel = new JPanel(new GridLayout(1, 2));
        JButton all = new JButton("All");
        all.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int index = 0;
                for (JCheckBox checkBox : checkBoxes) {
                    checkBox.setSelected(true);
                    EditTestsDialog.this.tests.set(index, EditTestsDialog.this.tests.get(index).setActive(true));
                    index++;
                }
                setSelectedTest(currentTest);
            }
        });
        upperButtonsPanel.add(all);
        JButton none = new JButton("None");
        none.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                int index = 0;
                for (JCheckBox checkBox : checkBoxes) {
                    checkBox.setSelected(false);
                    EditTestsDialog.this.tests.set(index, EditTestsDialog.this.tests.get(index).setActive(false));
                    index++;
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
                checkBoxesPanel.add(createCheckBox(test));
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
                while (checkBoxes.size() > currentTest) {
                    checkBoxesPanel.remove(checkBoxes.get(currentTest));
                    checkBoxes.remove(currentTest);
                }
                EditTestsDialog.this.tests.remove(currentTest);
                int size = EditTestsDialog.this.tests.size();
                for (int i = currentTest; i < size; i++) {
                    Test test = EditTestsDialog.this.tests.get(i);
                    test = new Test(test.input, test.output, i, test.active);
                    EditTestsDialog.this.tests.set(i, test);
                    checkBoxesPanel.add(createCheckBox(test));
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
        mainPanel.add(selectorAndButtonsPanel);
        JPanel testPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.add(new JLabel("Input:"), BorderLayout.NORTH);
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
        input = new JTextArea();
        input.setFont(Font.decode(Font.MONOSPACED));
        input.getDocument().addDocumentListener(listener);
        inputPanel.add(new JBScrollPane(input), BorderLayout.CENTER);
        outputPanel = new JPanel(new BorderLayout());
        outputPanel.add(new JLabel("Output:"), BorderLayout.NORTH);
        output = new JTextArea();
        output.setFont(Font.decode(Font.MONOSPACED));
        output.getDocument().addDocumentListener(listener);
        outputPanel.add(new JBScrollPane(output), BorderLayout.CENTER);
        knowAnswer = new JCheckBox("Know answer?");
        knowAnswer.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveCurrentTest();
            }
        });
        JPanel outputAndCheckBoxPanel = new JPanel(new BorderLayout());
        outputAndCheckBoxPanel.add(knowAnswer, BorderLayout.NORTH);
        outputAndCheckBoxPanel.add(outputPanel, BorderLayout.CENTER);
        testPanel.add(inputPanel);
        testPanel.add(outputAndCheckBoxPanel);
        mainPanel.add(testPanel);
        // Select first test if any exists, otherwise -1.
        setSelectedTest(this.tests.isEmpty() ? -1 : 0);
        // HiDPI-scaled default size, but user can resize freely.
        mainPanel.setPreferredSize(JBUI.size(700, 450));
        return mainPanel;
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

    private JCheckBox createCheckBox(final Test test) {
        final JCheckBox checkBox = new JCheckBox("", test.active);
        Dimension preferredSize = new Dimension(checkBox.getPreferredSize().width, HEIGHT);
        checkBox.setPreferredSize(preferredSize);
        checkBox.setMaximumSize(preferredSize);
        checkBox.setMinimumSize(preferredSize);
        checkBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                tests.set(test.index, tests.get(test.index).setActive(checkBox.isSelected()));
                setSelectedTest(currentTest);
            }
        });
        checkBoxes.add(checkBox);
        return checkBox;
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
        testList.setListData(tests.toArray());
        if (testList.getSelectedIndex() != currentTest) {
            testList.setSelectedIndex(currentTest);
        }
        testList.repaint();
        checkBoxesPanel.repaint();
        updating = false;
    }

    private void saveCurrentTest() {
        if (currentTest == -1) {
            return;
        }
        tests.set(currentTest, new Test(input.getText(), knowAnswer.isSelected() ? output.getText() : null, currentTest,
                checkBoxes.get(currentTest).isSelected()));
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

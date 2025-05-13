package hae.component.board;

import burp.api.montoya.MontoyaApi;
import hae.Config;
import hae.cache.DataCache;
import hae.component.board.message.MessageTableModel;
import hae.component.board.message.MessageTableModel.MessageTable;
import hae.component.board.table.Datatable;
import hae.utils.ConfigLoader;
import hae.utils.UIEnhancer;
import hae.utils.string.StringProcessor;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class Databoard extends JPanel {
    private static Boolean isMatchHost = false;
    private final MontoyaApi api;
    private final ConfigLoader configLoader;
    private final MessageTableModel messageTableModel;
    private final DefaultComboBoxModel comboBoxModel = new DefaultComboBoxModel();
    private final JComboBox hostComboBox = new JComboBox(comboBoxModel);
    private JTextField hostTextField;
    private JTabbedPane dataTabbedPane;
    private JSplitPane splitPane;
    private MessageTable messageTable;
    private JProgressBar progressBar;
    private SwingWorker<Map<String, List<String>>, Void> handleComboBoxWorker;
    private SwingWorker<Void, Void> applyHostFilterWorker;

    public Databoard(MontoyaApi api, ConfigLoader configLoader, MessageTableModel messageTableModel) {
        this.api = api;
        this.configLoader = configLoader;
        this.messageTableModel = messageTableModel;

        initComponents();
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        ((GridBagLayout) getLayout()).columnWidths = new int[]{25, 0, 0, 0, 20, 0};
        ((GridBagLayout) getLayout()).rowHeights = new int[]{0, 65, 20, 0, 0};
        ((GridBagLayout) getLayout()).columnWeights = new double[]{0.0, 0.0, 1.0, 0.0, 0.0, 1.0E-4};
        ((GridBagLayout) getLayout()).rowWeights = new double[]{0.0, 1.0, 0.0, 0.0, 1.0E-4};
        JLabel hostLabel = new JLabel("Host:");

        JButton clearDataButton = new JButton("Clear data");
        JButton clearCacheButton = new JButton("Clear cache");
        JButton actionButton = new JButton("Action");
        JPanel menuPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        menuPanel.setBorder(BorderFactory.createEmptyBorder(3, 3, 3, 3));
        JPopupMenu menu = new JPopupMenu();
        menuPanel.add(clearDataButton);
        menuPanel.add(clearCacheButton);
        menu.add(menuPanel);

        hostTextField = new JTextField();
        String defaultText = "Please enter the host";
        UIEnhancer.setTextFieldPlaceholder(hostTextField, defaultText);
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);

        dataTabbedPane = new JTabbedPane(JTabbedPane.TOP);
        dataTabbedPane.setPreferredSize(new Dimension(500, 0));
        dataTabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

        actionButton.addActionListener(e -> {
            int x = 0;
            int y = actionButton.getHeight();
            menu.show(actionButton, x, y);
        });

        clearDataButton.addActionListener(this::clearDataActionPerformed);
        clearCacheButton.addActionListener(this::clearCacheActionPerformed);

        progressBar = new JProgressBar();
        splitPane.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                resizePanel();
            }
        });

        splitPane.setVisible(false);
        progressBar.setVisible(false);

        add(hostLabel, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(8, 0, 5, 5), 0, 0));
        add(hostTextField, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(8, 0, 5, 5), 0, 0));
        add(actionButton, new GridBagConstraints(3, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(8, 0, 5, 5), 0, 0));

        add(splitPane, new GridBagConstraints(1, 1, 3, 1, 0.0, 1.0,
                GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(0, 5, 0, 5), 0, 0));
        add(progressBar, new GridBagConstraints(1, 2, 3, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 5, 0, 5), 0, 0));
        hostComboBox.setMaximumRowCount(5);
        add(hostComboBox, new GridBagConstraints(2, 0, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
                new Insets(8, 0, 5, 5), 0, 0));

        setAutoMatch();
    }

    private void resizePanel() {
        splitPane.setDividerLocation(0.4);
        TableColumnModel columnModel = messageTable.getColumnModel();
        int totalWidth = (int) (getWidth() * 0.6);
        columnModel.getColumn(0).setPreferredWidth((int) (totalWidth * 0.1));
        columnModel.getColumn(1).setPreferredWidth((int) (totalWidth * 0.3));
        columnModel.getColumn(2).setPreferredWidth((int) (totalWidth * 0.3));
        columnModel.getColumn(3).setPreferredWidth((int) (totalWidth * 0.1));
        columnModel.getColumn(4).setPreferredWidth((int) (totalWidth * 0.1));
        columnModel.getColumn(5).setPreferredWidth((int) (totalWidth * 0.1));
    }

    private void setProgressBar(boolean status) {
        progressBar.setIndeterminate(status);
        if (!status) {
            progressBar.setMaximum(100);
            progressBar.setString("OK");
            progressBar.setStringPainted(true);
            progressBar.setValue(progressBar.getMaximum());
        } else {
            progressBar.setString("Loading...");
            progressBar.setStringPainted(true);
        }
    }

    private void setAutoMatch() {
        hostComboBox.setSelectedItem(null);
        hostComboBox.addActionListener(this::handleComboBoxAction);

        hostTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                handleKeyEvents(e);
            }
        });

        hostTextField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterComboBoxList();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterComboBoxList();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterComboBoxList();
            }

        });
    }

    private void handleComboBoxAction(ActionEvent e) {
        if (!isMatchHost && hostComboBox.getSelectedItem() != null) {
            String selectedHost = hostComboBox.getSelectedItem().toString();

            if (getHostByList().contains(selectedHost)) {
                progressBar.setVisible(true);
                setProgressBar(true);
                hostTextField.setText(selectedHost);

                if (handleComboBoxWorker != null && !handleComboBoxWorker.isDone()) {
                    handleComboBoxWorker.cancel(true);
                }

                handleComboBoxWorker = new SwingWorker<>() {
                    @Override
                    protected Map<String, List<String>> doInBackground() {
                        return getSelectedMapByHost(selectedHost);
                    }

                    @Override
                    protected void done() {
                        if (!isCancelled()) {
                            try {
                                Map<String, List<String>> selectedDataMap = get();
                                if (!selectedDataMap.isEmpty()) {
                                    dataTabbedPane.removeAll();

                                    for (Map.Entry<String, List<String>> entry : selectedDataMap.entrySet()) {
                                        String tabTitle = String.format("%s (%s)", entry.getKey(), entry.getValue().size());
                                        Datatable datatablePanel = new Datatable(api, configLoader, entry.getKey(), entry.getValue());
                                        datatablePanel.setTableListener(messageTableModel);
                                        dataTabbedPane.addTab(tabTitle, datatablePanel);
                                    }

                                    JSplitPane messageSplitPane = messageTableModel.getSplitPane();
                                    splitPane.setLeftComponent(dataTabbedPane);
                                    splitPane.setRightComponent(messageSplitPane);
                                    messageTable = messageTableModel.getMessageTable();
                                    resizePanel();

                                    splitPane.setVisible(true);
                                    hostTextField.setText(selectedHost);

                                    hostComboBox.setPopupVisible(false);
                                    applyHostFilter(selectedHost);

                                    setProgressBar(false);
                                }
                            } catch (Exception ignored) {
                            }
                        }
                    }
                };

                handleComboBoxWorker.execute();
            }
        }
    }

    private void handleKeyEvents(KeyEvent e) {
        isMatchHost = true;
        int keyCode = e.getKeyCode();

        if (keyCode == KeyEvent.VK_SPACE && hostComboBox.isPopupVisible()) {
            e.setKeyCode(KeyEvent.VK_ENTER);
        }

        if (Arrays.asList(KeyEvent.VK_DOWN, KeyEvent.VK_UP).contains(keyCode)) {
            hostComboBox.dispatchEvent(e);
        }

        if (keyCode == KeyEvent.VK_ENTER) {
            isMatchHost = false;
            handleComboBoxAction(null);
        }

        if (keyCode == KeyEvent.VK_ESCAPE) {
            hostComboBox.setPopupVisible(false);
        }

        isMatchHost = false;
    }

    private Map<String, List<String>> getSelectedMapByHost(String selectedHost) {
        ConcurrentHashMap<String, Map<String, List<String>>> dataMap = Config.globalDataMap;
        Map<String, List<String>> selectedDataMap;

        if (selectedHost.contains("*")) {
            selectedDataMap = new HashMap<>();
            dataMap.keySet().forEach(key -> {
                if ((StringProcessor.matchesHostPattern(key, selectedHost) || selectedHost.equals("*")) && !key.contains("*")) {
                    Map<String, List<String>> ruleMap = dataMap.get(key);
                    for (String ruleKey : ruleMap.keySet()) {
                        List<String> dataList = ruleMap.get(ruleKey);
                        if (selectedDataMap.containsKey(ruleKey)) {
                            List<String> mergedList = new ArrayList<>(selectedDataMap.get(ruleKey));
                            mergedList.addAll(dataList);
                            HashSet<String> uniqueSet = new HashSet<>(mergedList);
                            selectedDataMap.put(ruleKey, new ArrayList<>(uniqueSet));
                        } else {
                            selectedDataMap.put(ruleKey, dataList);
                        }
                    }
                }
            });
        } else {
            selectedDataMap = dataMap.get(selectedHost);
        }

        return selectedDataMap;
    }

    private void filterComboBoxList() {
        isMatchHost = true;
        comboBoxModel.removeAllElements();
        String input = hostTextField.getText().toLowerCase();

        if (!input.isEmpty()) {
            for (String host : getHostByList()) {
                String lowerCaseHost = host.toLowerCase();
                if (lowerCaseHost.contains(input)) {
                    if (lowerCaseHost.equals(input)) {
                        comboBoxModel.insertElementAt(lowerCaseHost, 0);
                        comboBoxModel.setSelectedItem(lowerCaseHost);
                    } else {
                        comboBoxModel.addElement(host);
                    }
                }
            }
        }

        hostComboBox.setPopupVisible(comboBoxModel.getSize() > 0);
        isMatchHost = false;
    }

    private void applyHostFilter(String filterText) {
        TableRowSorter<TableModel> sorter = (TableRowSorter<TableModel>) messageTable.getRowSorter();
        String cleanedText = StringProcessor.replaceFirstOccurrence(filterText, "*.", "");

        if (applyHostFilterWorker != null && !applyHostFilterWorker.isDone()) {
            applyHostFilterWorker.cancel(true);
        }

        applyHostFilterWorker = new SwingWorker<>() {
            @Override
            protected Void doInBackground() {
                RowFilter<Object, Object> rowFilter = new RowFilter<>() {
                    public boolean include(Entry<?, ?> entry) {
                        if (cleanedText.equals("*")) {
                            return true;
                        } else {
                            String host = StringProcessor.getHostByUrl((String) entry.getValue(1));
                            return StringProcessor.matchesHostPattern(host, filterText);
                        }
                    }
                };

                sorter.setRowFilter(rowFilter);
                messageTableModel.applyHostFilter(filterText);

                return null;
            }
        };

        applyHostFilterWorker.execute();
    }

    private List<String> getHostByList() {
        List<String> result = new ArrayList<>();
        if (!Config.globalDataMap.isEmpty()) {
            result = new ArrayList<>(Config.globalDataMap.keySet());
        }

        return result;
    }

    private void clearCacheActionPerformed(ActionEvent e) {
        int retCode = JOptionPane.showConfirmDialog(this, "Do you want to clear cache?", "Info",
                JOptionPane.YES_NO_OPTION);
        if (retCode == JOptionPane.YES_OPTION) {
            DataCache.clear();
        }
    }

    private void clearDataActionPerformed(ActionEvent e) {
        int retCode = JOptionPane.showConfirmDialog(this, "Do you want to clear data?", "Info",
                JOptionPane.YES_NO_OPTION);
        String host = hostTextField.getText();
        if (retCode == JOptionPane.YES_OPTION && !host.isEmpty()) {
            dataTabbedPane.removeAll();
            splitPane.setVisible(false);
            progressBar.setVisible(false);

            Config.globalDataMap.keySet().parallelStream().forEach(key -> {
                if (StringProcessor.matchesHostPattern(key, host) || host.equals("*")) {
                    Config.globalDataMap.remove(key);
                }
            });

            // 删除无用的数据
            Set<String> wildcardKeys = Config.globalDataMap.keySet().stream()
                    .filter(key -> key.startsWith("*."))
                    .collect(Collectors.toSet());

            Set<String> existingSuffixes = Config.globalDataMap.keySet().stream()
                    .filter(key -> !key.startsWith("*."))
                    .map(key -> {
                        int dotIndex = key.indexOf(".");
                        return dotIndex != -1 ? key.substring(dotIndex) : "";
                    })
                    .collect(Collectors.toSet());

            Set<String> keysToRemove = wildcardKeys.stream()
                    .filter(key -> !existingSuffixes.contains(key.substring(1)))
                    .collect(Collectors.toSet());

            keysToRemove.forEach(Config.globalDataMap::remove);

            if (Config.globalDataMap.size() == 1 && Config.globalDataMap.keySet().stream().anyMatch(key -> key.equals("*"))) {
                Config.globalDataMap.remove("*");
            }

            messageTableModel.deleteByHost(host);

            hostTextField.setText("");
        }
    }
}

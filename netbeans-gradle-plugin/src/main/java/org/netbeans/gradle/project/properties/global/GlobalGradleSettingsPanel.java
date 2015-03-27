package org.netbeans.gradle.project.properties.global;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.ListModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.jtrim.property.BoolProperties;
import org.jtrim.property.PropertySource;

@SuppressWarnings("serial")
public class GlobalGradleSettingsPanel extends javax.swing.JPanel implements GlobalSettingsEditor {
    public GlobalGradleSettingsPanel() {
        initComponents();

        DefaultListModel<CategoryItem> categoriesModel = new DefaultListModel<>();
        categoriesModel.addElement(new CategoryItem("Gradle Installation", new GradleInstallationPanel()));
        categoriesModel.addElement(new CategoryItem("Platform priority", new PlatformPriorityPanel(false)));
        categoriesModel.addElement(new CategoryItem("Script & tasks", new ScriptAndTasksPanel()));
        categoriesModel.addElement(new CategoryItem("Build script parsing", new BuildScriptParsingPanel()));
        categoriesModel.addElement(new CategoryItem("Task execution", new TaskExecutionPanel()));
        categoriesModel.addElement(new CategoryItem("Other", new OtherOptionsPanel()));

        jCategoriesList.setModel(categoriesModel);

        jCategoriesList.setSelectedIndex(0);
        showSelectedEditor();

        jCategoriesList.addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                showSelectedEditor();
            }
        });
    }

    private void showSelectedEditor() {
        jCurrentCategoryPanel.removeAll();

        CategoryItem selected = jCategoriesList.getSelectedValue();
        if (selected != null) {
            JComponent editorComponent = selected.editor.getEditorComponent();
            jCurrentCategoryPanel.add(editorComponent);
        }

        jCurrentCategoryPanel.revalidate();
        jCurrentCategoryPanel.repaint();
    }

    @Override
    public final void updateSettings() {
        ListModel<CategoryItem> model = jCategoriesList.getModel();
        int categoryCount = model.getSize();
        for (int i = 0; i < categoryCount; i++) {
            model.getElementAt(i).editor.updateSettings();
        }
    }

    @Override
    public final void saveSettings() {
        ListModel<CategoryItem> model = jCategoriesList.getModel();
        int categoryCount = model.getSize();
        for (int i = 0; i < categoryCount; i++) {
            model.getElementAt(i).editor.saveSettings();
        }
    }

    @Override
    public PropertySource<Boolean> valid() {
        ListModel<CategoryItem> model = jCategoriesList.getModel();
        int categoryCount = model.getSize();

        @SuppressWarnings("unchecked")
        PropertySource<Boolean>[] subValids = (PropertySource<Boolean>[])new PropertySource<?>[categoryCount];
        for (int i = 0; i < categoryCount; i++) {
            subValids[i] = model.getElementAt(i).editor.valid();
        }

        return BoolProperties.or(subValids);
    }

    @Override
    public JComponent getEditorComponent() {
        return this;
    }

    private static final class CategoryItem {
        private final String caption;
        public final GlobalSettingsEditor editor;

        public CategoryItem(String caption, GlobalSettingsEditor editor) {
            this.caption = caption;
            this.editor = editor;
        }

        @Override
        public String toString() {
            return caption;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form. WARNING: Do NOT modify this code. The
     * content of this method is always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jCategoriesLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jCategoriesList = new javax.swing.JList<CategoryItem>();
        jCurrentCategoryPanel = new javax.swing.JPanel();

        org.openide.awt.Mnemonics.setLocalizedText(jCategoriesLabel, org.openide.util.NbBundle.getMessage(GlobalGradleSettingsPanel.class, "GlobalGradleSettingsPanel.jCategoriesLabel.text")); // NOI18N

        jScrollPane1.setViewportView(jCategoriesList);

        jCurrentCategoryPanel.setLayout(new java.awt.GridLayout(1, 1));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 125, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jCurrentCategoryPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 257, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jCategoriesLabel)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jCategoriesLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jCurrentCategoryPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 266, Short.MAX_VALUE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jCategoriesLabel;
    private javax.swing.JList<CategoryItem> jCategoriesList;
    private javax.swing.JPanel jCurrentCategoryPanel;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}

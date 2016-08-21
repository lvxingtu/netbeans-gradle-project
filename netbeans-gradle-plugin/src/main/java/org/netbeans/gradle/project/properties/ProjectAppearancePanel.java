package org.netbeans.gradle.project.properties;

import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.ProfileValuesEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileValuesEditorFactory;

@SuppressWarnings("serial")
public class ProjectAppearancePanel extends javax.swing.JPanel {
    private final ProjectNodeNamePanel nodeNamePanel;

    public ProjectAppearancePanel() {
        initComponents();

        nodeNamePanel = new ProjectNodeNamePanel(true);
        jProjectNodeNameHolder.add(nodeNamePanel);
        nodeNamePanel.setVisible(true);
    }

    public static ProfileBasedPanel createProfileBasedPanel(final NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        final ProjectAppearancePanel customPanel = new ProjectAppearancePanel();
        return ProfileBasedPanel.createPanel(project, customPanel, new ProfileValuesEditorFactory() {
            @Override
            public ProfileValuesEditor startEditingProfile(String displayName, ActiveSettingsQuery profileQuery) {
                return customPanel.new PropertyValues(profileQuery);
            }
        });
    }

    private final class PropertyValues implements ProfileValuesEditor {
        public final PropertyReference<String> displayNamePatternRef;
        private String displayNamePattern;

        public PropertyValues(ActiveSettingsQuery settings) {
            this.displayNamePatternRef = NbGradleCommonProperties.displayNamePattern(settings);
            this.displayNamePattern = displayNamePatternRef.tryGetValueWithoutFallback();
        }

        @Override
        public void displayValues() {
            nodeNamePanel.updatePattern(displayNamePattern, displayNamePatternRef);
        }

        @Override
        public void readFromGui() {
            displayNamePattern = nodeNamePanel.getNamePattern();
        }

        @Override
        public void applyValues() {
            displayNamePatternRef.setValue(displayNamePattern);
        }
    }

    /**
     * This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jProjectNodeNameHolder = new javax.swing.JPanel();

        setLayout(new java.awt.GridLayout(1, 1));

        jProjectNodeNameHolder.setLayout(new java.awt.GridLayout(1, 1));
        add(jProjectNodeNameHolder);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel jProjectNodeNameHolder;
    // End of variables declaration//GEN-END:variables
}

package org.netbeans.gradle.project;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.support.CommonProjectActions;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.actions.Presenter;
import org.openide.util.lookup.ProxyLookup;

public final class GradleProjectLogicalViewProvider implements LogicalViewProvider {
    private static final Logger LOGGER = Logger.getLogger(GradleProjectLogicalViewProvider.class.getName());

    private final NbGradleProject project;

    public GradleProjectLogicalViewProvider(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    @Override
    public Node createLogicalView() {
        DataFolder projectFolder = DataFolder.findFolder(project.getProjectDirectory());
        GradleProjectNode result = new GradleProjectNode(projectFolder.getNodeDelegate());
        result.scanProject();
        return result;
    }

    private Children createChildren() {
        return Children.create(new GradleProjectChildFactory(project), true);
    }

    private Lookup createLookup(Node rootNode) {
        return new ProxyLookup(
                project.getLookup(),
                rootNode.getLookup());
    }

    private final class GradleProjectNode extends FilterNode {
        private final TasksActionMenu tasksAction;
        private final Action[] actions;

        public GradleProjectNode(Node node) {
            super(node, createChildren(), createLookup(node));

            this.tasksAction = new TasksActionMenu(project);
            this.actions = new Action[] {
                //ProjectSensitiveActions.projectCommandAction(ActionProvider.COMMAND_RUN, "Run", null),
                CommonProjectActions.newFileAction(),
                this.tasksAction,
                CommonProjectActions.closeProjectAction(),
            };
        }

        public void scanProject() {
            tasksAction.scanForTasks();
        }

        @Override
        public Action[] getActions(boolean context) {
            return actions.clone();
        }

        @Override
        public Image getIcon(int type) {
            return NbIcons.getGradleIcon();
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(type);
        }

        @Override
        public String getDisplayName() {
            return project.getDisplayName();
        }
    }

    @Override
    public Node findPath(Node root, Object target) {
        //leave unimplemented for now
        return null;
    }

    @SuppressWarnings("serial") // don't care about serialization
    private static class TasksActionMenu extends AbstractAction implements Presenter.Popup {
        private final NbGradleProject project;
        private final JMenu tasksMenu;
        private NbGradleModel lastUsedModel; // used only on the EDT.

        public TasksActionMenu(NbGradleProject project) {
            this.project = project;
            this.tasksMenu = new JMenu("Tasks");
            this.lastUsedModel = null;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            scanForTasks();
        }

        public void scanForTasks() {
            NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
                @Override
                public void run() {
                    NbGradleModel projectModel = project.getCurrentModel();
                    if (lastUsedModel == projectModel) {
                        return;
                    }
                    lastUsedModel = projectModel;

                    final Collection<String> tasks = projectModel.getMainModule().getTasks();

                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            tasksMenu.removeAll();
                            for (final String task: tasks) {
                                tasksMenu.add(task).addActionListener(new ActionListener() {
                                    @Override
                                    public void actionPerformed(ActionEvent e) {
                                        GradleTasks.createAsyncGradleTask(project, task).run();
                                    }
                                });
                            }
                        }
                    });
                }
            });
        }

        @Override
        public JMenuItem getPopupPresenter() {
            return tasksMenu;
        }
    }
}

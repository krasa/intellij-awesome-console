package awesome.console.config;

import awesome.console.AwesomeLinkFilterProvider;
import com.intellij.execution.filters.ConsoleFilterProvider;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.util.xmlb.XmlSerializerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@State(
		name = "Awesome Console Config",
		storages = {
				@Storage(value = "awesomeconsole.xml", roamingType = RoamingType.DISABLED)
		}
)
public class AwesomeConsoleConfig implements PersistentStateComponent<AwesomeConsoleConfig> {
	public boolean SPLIT_ON_LIMIT = false;
	public boolean LIMIT_LINE_LENGTH = true;
	public int LINE_MAX_LENGTH = 1024;
	public boolean SEARCH_URLS = true;

	public AwesomeConsoleConfig() {
		ApplicationManager.getApplication().getMessageBus().connect().subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
			@Override
			public void projectClosed(@NotNull Project project) {
				ConsoleFilterProvider.FILTER_PROVIDERS.getExtensionList().forEach(consoleFilterProvider -> {
					if (consoleFilterProvider instanceof AwesomeLinkFilterProvider) {
						((AwesomeLinkFilterProvider) consoleFilterProvider).projectClosed(project);
					}
				});
			}
		});
	}

	/**
	 * PersistentStateComponent
	 */
	@Nullable
	@Override
	public AwesomeConsoleConfig getState() {
		return this;
	}

	@Override
	public void loadState(@NotNull final AwesomeConsoleConfig state) {
		XmlSerializerUtil.copyBean(state, this);
	}

	/**
	 * Helpers
	 */
	public static AwesomeConsoleConfig getInstance() {
		return ServiceManager.getService(AwesomeConsoleConfig.class);
	}

}

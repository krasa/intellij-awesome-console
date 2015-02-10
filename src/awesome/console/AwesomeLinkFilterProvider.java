package awesome.console;

import com.intellij.execution.filters.ConsoleFilterProvider;
import com.intellij.execution.filters.Filter;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class AwesomeLinkFilterProvider implements ConsoleFilterProvider {
	@NotNull
	@Override
	public Filter[] getDefaultFilters(@NotNull Project project) {
		Filter filter = new AwesomeLinkFilter(project);
		AwesomeClassLinkFilter linkFilter = new AwesomeClassLinkFilter(project);
		return new Filter[]{filter, linkFilter};
	}
}

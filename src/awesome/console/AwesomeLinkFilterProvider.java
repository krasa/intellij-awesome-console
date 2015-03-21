package awesome.console;

import com.intellij.execution.filters.*;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.project.Project;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

public class AwesomeLinkFilterProvider extends ConsoleDependentFilterProvider {
    @NotNull
    @Override
    public Filter[] getDefaultFilters(@NotNull ConsoleView consoleView, @NotNull Project project, @NotNull GlobalSearchScope scope) {
        return new Filter[] { new AwesomeClassLinkFilter(project,consoleView) };
    }

    @NotNull
	@Override
	public Filter[] getDefaultFilters(@NotNull Project project) {
		Filter filter = new AwesomeLinkFilter(project);
		return new Filter[]{filter};
	}
}

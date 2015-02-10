package awesome.console;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import awesome.console.config.AwesomeConsoleConfig;

import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.HyperlinkInfoFactory;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;

public class AwesomeClassLinkFilter implements Filter {
	/**fully qualified without stacktraces*/
	private static final Pattern PATTERN = Pattern.compile("([a-zA-Z_$][a-zA-Z\\d_$]*\\.)+[a-zA-Z_$][a-zA-Z\\d_$]+(?![a-zA-Z\\d_$\\.]*[\\.:(])");
	private final Project project;
	private PsiShortNamesCache instance;
	private AwesomeConsoleConfig config;
	protected GlobalSearchScope globalSearchScope;

	public AwesomeClassLinkFilter(final Project project) {
		this.project = project;
		this.instance = PsiShortNamesCache.getInstance(project);
		config = AwesomeConsoleConfig.getInstance();
		globalSearchScope = GlobalSearchScope.allScope(project);
	}

	@Override
	public Result applyFilter(final String line, final int endPoint) {
		final int startPoint = endPoint - line.length();
		final String chunk = splitLine(line);
		List<ResultItem> resultItemsFile = getResultItemsFile(chunk, startPoint);
		if (resultItemsFile == null) {
			return null;
		}
		return new Result(resultItemsFile);
	}

	public String splitLine(final String line) {
		if (!config.LIMIT_LINE_LENGTH || config.LINE_MAX_LENGTH < 0) {
			return line;
		}
		final int length = line.length();
		if (config.LINE_MAX_LENGTH > length) {
			return line;
		}
		return line.substring(0, config.LINE_MAX_LENGTH);
	}

	public List<ResultItem> getResultItemsFile(final String line, final int startPoint) {
		final List<ResultItem> results = new ArrayList<>();
		final Matcher matcher = PATTERN.matcher(line);
		while (matcher.find()) {
			final String match = matcher.group();
			List<PsiClass> classes = findClasses(match);
			if (classes == null || classes.isEmpty()) {
				continue;
			}

			final List<VirtualFile> virtualFiles = getVirtualFiles(classes);
			if (virtualFiles.isEmpty()) {
				continue;
			}
			// todo must implement my own which can navigate properly
			final HyperlinkInfo linkInfo = HyperlinkInfoFactory.getInstance().createMultipleFilesHyperlinkInfo(
					virtualFiles, -1, project);
			results.add(new Result(startPoint + matcher.start(), startPoint + matcher.end(), linkInfo));
		}
		return results;
	}

	@NotNull
	private List<VirtualFile> getVirtualFiles(List<PsiClass> classes) {
		List<VirtualFile> files = new ArrayList<>();
		for (int i = 0; i < classes.size(); i++) {
			PsiClass aClass = classes.get(i);
			files.add(aClass.getContainingFile().getVirtualFile());
		}
		return files;
	}

	@Nullable
	private List<PsiClass> findClasses(String fullName) {
		// todo proper field/method handling

		 fullName = StringUtils.substringBefore(fullName, "$");
		String className = StringUtils.substringAfterLast(fullName, ".");
		PsiClass[] classesByName = getClassesByName(className);
		if (classesByName.length == 0) {
			fullName = StringUtils.substringBeforeLast(fullName, ".");
			className = StringUtils.substringAfterLast(fullName, ".");
			classesByName = getClassesByName(className);
			if (classesByName.length == 0) {
				return null;
			}
		}

		List<PsiClass> files = new ArrayList<>();
		String regex = null;
		for (int i = 0; i < classesByName.length; i++) {
			PsiClass psiClass = classesByName[i];
			String qualifiedName = psiClass.getQualifiedName();
			if (regex == null) {
				regex = fullName.replace(".", "[a-zA-Z\\d_$]*\\.");
			}
			if (qualifiedName != null && qualifiedName.matches(regex)) {
				files.add(psiClass);
			}
		}
		return files;
	}

	@NotNull
	private PsiClass[] getClassesByName(String className) {
		return instance.getClassesByName(className, globalSearchScope);
	}

}
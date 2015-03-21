package awesome.console;

import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.util.Disposer;
import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.*;

import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.*;

import awesome.console.config.AwesomeConsoleConfig;

import com.intellij.execution.filters.*;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.HighlighterColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.project.*;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.impl.CompositeShortNamesCache;
import com.intellij.psi.search.*;
import com.intellij.util.containers.HashSet;
import com.intellij.util.ui.UIUtil;

public class AwesomeClassLinkFilter implements Filter, Disposable {

	/** fully qualified without stacktraces */
	static final Pattern PATTERN = Pattern.compile("([a-zA-Z_$][a-zA-Z\\d_$]*\\.)+[a-zA-Z_$][a-zA-Z\\d_$]+(?![a-zA-Z\\d_$\\.]*[\\.:(])");
	private final Project project;
	private AwesomeConsoleConfig config;
	protected GlobalSearchScope globalSearchScope;
	protected TextAttributes attributes;
	private PsiShortNamesCache shortNamesCache;
	private HashSet<String> dest = new HashSet<>();
	private volatile boolean disposed;

	public AwesomeClassLinkFilter(final Project project, ConsoleView consoleView) {
		this.project = project;
		config = AwesomeConsoleConfig.getInstance();
		globalSearchScope = GlobalSearchScope.allScope(project);
		shortNamesCache = CompositeShortNamesCache.getInstance(project);
		initCache();

		// attributes =
		// EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.HYPERLINK_ATTRIBUTES);
		Color libTextColor = UIUtil.getInactiveTextColor();
		attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(HighlighterColors.TEXT);
		attributes = attributes.clone();
        attributes.setBackgroundColor(null);
		attributes.setForegroundColor(libTextColor);
//		 attributes.setEffectColor(libTextColor);
        Disposer.register(consoleView, this);
    }

	private void initCache() {
		DumbService.getInstance(project).smartInvokeLater(new Runnable() {

			@Override
			public void run() {
				if (!disposed) {
					shortNamesCache.getAllClassNames(dest);
				}
			}
		});

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
			results.add(new Result(startPoint + matcher.start(), startPoint + matcher.end(), linkInfo, attributes));
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
		// long start = System.nanoTime();
		PsiClass[] classesByName = PsiClass.EMPTY_ARRAY;
		if (dest.contains(className)) {
			classesByName = this.shortNamesCache.getClassesByName(className, globalSearchScope);
		}
		// System.err.println(System.nanoTime() - start);
		return classesByName;
	}

	@Override
	public void dispose() {
		disposed = true;
	}
}
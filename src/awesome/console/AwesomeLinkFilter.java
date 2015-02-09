package awesome.console;

import awesome.console.config.AwesomeConsoleConfig;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.execution.filters.HyperlinkInfoFactory;
import com.intellij.ide.browsers.OpenUrlHyperlinkInfo;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AwesomeLinkFilter implements Filter {
	private static final Pattern FILE_PATTERN = Pattern.compile("([a-zA-Z0-9][a-zA-Z0-9/\\-_\\.]*\\.[a-zA-Z0-9\\-_\\.]+)(:(\\d+))?");
	private static final Pattern URL_PATTERN = Pattern.compile("((((ftp)|(file)|(https?)):/)?/[-_.!~*\\\\'()a-zA-Z0-9;\\\\/?:\\\\@&=+\\\\$,%#]+)");
	private static final int LINE_MAX_LENGTH = 1024;
	private final Map<String, List<File>> fileCache = new HashMap<>();
	private final Map<String, List<File>> fileBaseCache = new HashMap<>();
	private final Project project;
	private final List<String> srcRoots;

	public AwesomeLinkFilter(final Project project) {
		this.project = project;
//		FILE_PATTERN = Pattern.compile(AwesomeConsoleConfig.getInstance().FILE_PATTERN);
		System.err.println("state.FILE_PATTERN: " + AwesomeConsoleConfig.getInstance().FILE_PATTERN);
		createFileCache(new File(project.getBasePath()));
		srcRoots = getSourceRoots();
		createFileCache(new File(project.getBasePath()));
	}

	@Override
	public Result applyFilter(final String line, final int endPoint) {
		final List<ResultItem> results = new ArrayList<>();
		final int startPoint = endPoint - line.length();
		final String chunk = splitLine(line);
		results.addAll(getResultItemsUrl(chunk, startPoint));
		results.addAll(getResultItemsFile(chunk, startPoint));
		return new Result(results);
	}

	public String splitLine(final String line) {
		final int length = line.length();
		if (LINE_MAX_LENGTH > length) {
			return line;
		}
		return line.substring(0, LINE_MAX_LENGTH);
	}

	public List<ResultItem> getResultItemsUrl(final String line, final int startPoint) {
		final List<ResultItem> results = new ArrayList<>();
		final Matcher matcher = URL_PATTERN.matcher(line);
		while (matcher.find()) {
			final String match = matcher.group(1);
			final String file = getFileFromUrl(match);
			if (null != file && !new File(file).exists()) {
				continue;
			}
			results.add(
					new Result(
							startPoint + matcher.start(),
							startPoint + matcher.end(),
							new OpenUrlHyperlinkInfo(match))
			);
		}
		return results;
	}

	public String getFileFromUrl(final String url) {
		if (url.startsWith("/")) {
			return url;
		}
		final String fileUrl = "file://";
		if(url.startsWith(fileUrl)) {
			return url.substring(fileUrl.length());
		}
		return null;
	}

	public List<ResultItem> getResultItemsFile(final String line, final int startPoint) {
		final List<ResultItem> results = new ArrayList<>();
		final Matcher matcher = FILE_PATTERN.matcher(line);
		while (matcher.find()) {
			final String match = matcher.group(1);
			final List<VirtualFile> virtualFiles = new ArrayList<>();
			List<File> matchingFiles = fileCache.get(match);
			if (null == matchingFiles) {
				matchingFiles = getResultItemsFileFromBasename(match);
				if (null == matchingFiles || 0 >= matchingFiles.size()) {
					continue;
				}
			}
			final VirtualFileSystem fileSystem = project.getBaseDir().getFileSystem();
			for (final File file : matchingFiles) {
				final VirtualFile virtualFile = fileSystem.findFileByPath(file.getPath());
				if (null == virtualFile) {
					continue;
				}
				virtualFiles.add(virtualFile);
			}
			if (0 >= virtualFiles.size()) {
				continue;
			}
			final HyperlinkInfo linkInfo = HyperlinkInfoFactory.getInstance().createMultipleFilesHyperlinkInfo(
					virtualFiles,
					matcher.group(3) == null ? 0 : Integer.parseInt(matcher.group(3)) - 1,
					project
			);
			results.add(
					new Result(
							startPoint + matcher.start(),
							startPoint + matcher.end(),
							linkInfo)
			);
		}
		return results;
	}

	public List<File> getResultItemsFileFromBasename(final String match) {
		final ArrayList<File> matches = new ArrayList<>();
		final char packageSeparator = '.';
		final int index = match.lastIndexOf(packageSeparator);
		if (-1 >= index) {
			return matches;
		}
		final String basename = match.substring(index + 1);
		if (0 >= basename.length()) {
			return matches;
		}
		if (!fileBaseCache.containsKey(basename)) {
			return matches;
		}
		final String path = match.substring(0, index).replace(packageSeparator, File.separatorChar);
		for (final File file : fileBaseCache.get(basename)) {
			final String parent = file.getParent();
			if (null == parent) {
				continue;
			}
			if (!matchSource(parent, path)) {
				continue;
			}
			matches.add(file);
		}
		return matches;
	}

	private void createFileCache(final File dir) {
		try {
			Files.walkFileTree(dir.toPath(), new ProjectFileVisitor<>(fileCache, fileBaseCache));
		} catch (final IOException e) {
			e.printStackTrace();
		}
	}

	private List<String> getSourceRoots() {
		final VirtualFile[] contentSourceRoots = ProjectRootManager.getInstance(project).getContentSourceRoots();
		final List<String> roots = new ArrayList<>();
		for (final VirtualFile root : contentSourceRoots) {
			roots.add(root.getPath());
		}
		return roots;
	}

	private boolean matchSource(final String parent, final String path) {
		for (final String srcRoot : srcRoots) {
			if ((srcRoot + File.separatorChar + path).equals(parent)) {
				return true;
			}
		}
		return false;
	}
}

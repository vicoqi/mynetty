package com.vic.mynetty.common.route;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Pattern;

/**
 * Usage:
 * 1) new Matcher(path).or(path).or(path).compile();
 * 2) new Matcher(pattern);
 *
 */
@Data
@AllArgsConstructor
public class Path implements Route {
	private String path;
	public static class Matcher implements RouteMatcher<Path> {
		private static final String WILDCARD = "*";
		private static final String FOLDER_WILDCARD = "**";
		private static final String FILE_WILDCARD = "*";
		private static final String FOLDER_WILDCARD_PATTERN_STR = "(\\w*\\/)*(\\w*){0,1}";
		private static final String FILE_WILDCARD_PATTERN_STR = "\\w*";
		@Getter
		private String path;
		@Getter
		private Pattern pattern;
		
		public Matcher() {}
		
		public Matcher(String path) {
			this.path = path;
		}
		
		public Matcher(Pattern pattern) {
			this.pattern = pattern;
		}
		
		public Matcher or(String path) {
			if (this.path == null || this.path.trim().length() == 0) {
				this.path = path;
			} else {
				this.path = this.path + "|" + path;
			}
			return this;
		}
		
		public Matcher compile() {
			if (StringUtils.isEmpty(this.path)) {
				return this;
			}
			String tmpPath = this.path;
			StringBuilder sb = new StringBuilder();
			while (tmpPath.contains(WILDCARD)) {
				int folderWildcardIndex = tmpPath.indexOf(FOLDER_WILDCARD);
				int fileWildcardIndex = tmpPath.indexOf(FILE_WILDCARD);
				if (folderWildcardIndex != -1 && (fileWildcardIndex == -1 || folderWildcardIndex <= fileWildcardIndex)) {
					sb.append(tmpPath.substring(0, folderWildcardIndex)).append(FOLDER_WILDCARD_PATTERN_STR);
					tmpPath = tmpPath.substring(folderWildcardIndex + 2);
					continue;
				}
				if (tmpPath.length() == 0) {
					break;
				}
				if (fileWildcardIndex != -1) {
					sb.append(tmpPath.substring(0, fileWildcardIndex)).append(FILE_WILDCARD_PATTERN_STR);
					tmpPath = tmpPath.substring(fileWildcardIndex + 1);
				}
			}
			sb.append(tmpPath);
			this.pattern = Pattern.compile(sb.toString());
			return this;
		}

		@Override
		public boolean matches(Path route) {
			return this.pattern != null && this.pattern.matcher(route.getPath()).matches();
		}

		@Override
		public String toString() {
			if (path != null) {
				return path;
			}
			if (pattern != null) {
				return pattern.toString();
			}
			return null;
		}
		
	}
}

package org.faudroids.mrhyde.jekyll;

import org.faudroids.mrhyde.git.AbstractNode;
import org.faudroids.mrhyde.git.DirNode;
import org.faudroids.mrhyde.git.FileManager;
import org.roboguice.shaded.goole.common.base.Optional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import rx.Observable;
import rx.functions.Func1;
import timber.log.Timber;

/**
 * Handles Jekyll specific tasks for one repository.
 */
public class JekyllManager {

	private static final String DIR_POSTS = "_posts";
	private static final Pattern POST_TITLE_PATTERN = Pattern.compile("(\\d\\d\\d\\d)-(\\d\\d)-(\\d\\d)(.*)\\..*");

	private final FileManager fileManager;

	JekyllManager(FileManager fileManager) {
		this.fileManager = fileManager;
	}


	/**
	 * Returns all posts sorted by date with the newest first.
	 */
	public Observable<List<Post>> getAllPosts() {
		return fileManager.getTree()
				.flatMap(new Func1<DirNode, Observable<List<Post>>>() {
					@Override
					public Observable<List<Post>> call(DirNode dirNode) {
						// check if post dir exists
						List<Post> posts = new ArrayList<>();
						if (!dirNode.getEntries().containsKey(DIR_POSTS)) return Observable.just(posts);

						// parse titles
						DirNode postsDir = (DirNode) dirNode.getEntries().get(DIR_POSTS);
						for (AbstractNode postNode : postsDir.getEntries().values()) {
							Optional<Post> post = parsePostTitle(postNode.getPath());
							if (post.isPresent()) posts.add(post.get());
						}

						// sort by date
						Collections.sort(posts);
						Collections.reverse(posts);

						return Observable.just(posts);
					}
				});
	}


	public List<String> getAllDrafts() {
		return new ArrayList<>();
	}


	public Optional<Post> parsePostTitle(String fileName) {
		// check for match
		Matcher matcher = POST_TITLE_PATTERN.matcher(fileName);
		if (!matcher.matches()) return Optional.absent();

		try {
			// get date
			int year = Integer.valueOf(matcher.group(1));
			int month = Integer.valueOf(matcher.group(2));
			int day = Integer.valueOf(matcher.group(3));
			Calendar calendar = Calendar.getInstance();
			calendar.set(year, month, day);

			// get title
			String title = matcher.group(4);
			title = title.replaceAll("-", " ");
			title = title.trim();
			title = Character.toUpperCase(title.charAt(0)) + title.substring(1);

			return Optional.of(new Post(title, calendar.getTime()));

		} catch (NumberFormatException nfe) {
			Timber.w(nfe, "failed to parse post tile \"" + fileName + "\"");
			return Optional.absent();
		}
	}

}

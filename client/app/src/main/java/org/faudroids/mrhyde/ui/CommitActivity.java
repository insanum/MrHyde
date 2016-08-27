package org.faudroids.mrhyde.ui;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.Transformation;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import org.faudroids.mrhyde.R;
import org.faudroids.mrhyde.app.MrHydeApp;
import org.faudroids.mrhyde.git.FileManager;
import org.faudroids.mrhyde.git.FileManagerFactory;
import org.faudroids.mrhyde.github.GitHubRepository;
import org.faudroids.mrhyde.ui.utils.AbstractActionBarActivity;
import org.faudroids.mrhyde.utils.DefaultErrorAction;
import org.faudroids.mrhyde.utils.DefaultTransformer;
import org.faudroids.mrhyde.utils.ErrorActionBuilder;
import org.faudroids.mrhyde.utils.HideSpinnerAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import rx.Observable;
import rx.functions.Action1;
import timber.log.Timber;

public final class CommitActivity extends AbstractActionBarActivity {

	static final String EXTRA_REPOSITORY = "EXTRA_REPOSITORY";

	private static final String
			STATE_FILES_EXPAND = "STATE_FILES_EXPAND",
			STATE_DIFF_EXPAND = "STATE_DIFF_EXPAND",
			STATE_MESSAGE_EXPAND = "STATE_MESSAGE_EXPAND";

	@Inject FileManagerFactory fileManagerFactory;

	@BindView(R.id.changed_files_expand) protected ImageButton changedFilesExpandButton;
	@BindView(R.id.changed_files_title) protected TextView changedFilesTitleView;
	@BindView(R.id.changed_files) protected TextView changedFilesView;

	@BindView(R.id.diff_expand) protected ImageButton diffExpandButton;
	@BindView(R.id.diff_title) protected TextView diffTitleView;
	@BindView(R.id.diff) protected TextView diffView;

	@BindView(R.id.message_expand) protected ImageButton messageExpandButton;
	@BindView(R.id.message_title) protected TextView messageTitleView;
	@BindView(R.id.message) protected  EditText messageView;

	@BindView(R.id.commit_button) protected Button commitButton;


	@Override
	public void onCreate(Bundle savedInstanceState) {
    ((MrHydeApp) getApplication()).getComponent().inject(this);
		super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_commit);
    ButterKnife.bind(this);

		setTitle(getString(R.string.title_commit));
		changedFilesTitleView.setText(getString(R.string.commit_changed_files, ""));
		final GitHubRepository repository = (GitHubRepository) getIntent().getSerializableExtra(EXTRA_REPOSITORY);
		final FileManager fileManager = fileManagerFactory.createFileManager(repository);

		// load file content
		compositeSubscription.add(Observable.zip(
				fileManager.getChangedFiles(),
				fileManager.getDeletedFiles(),
				fileManager.getDiff(),
        (changedFiles, deletedFiles, diff) -> new Change(changedFiles, deletedFiles, diff))
				.compose(new DefaultTransformer<Change>())
				.subscribe(change -> {
          // updated changed files list
          List<String> filesList = new ArrayList<>();
          filesList.addAll(change.changedFiles);
          for (String deletedFile : change.deletedFiles)
            filesList.add(getString(R.string.commit_deleted, deletedFile));
          Collections.sort(filesList);

          StringBuilder builder = new StringBuilder();
          for (String file : filesList) {
            builder.append(file).append('\n');
          }
          changedFilesView.setText(builder.toString());
          changedFilesTitleView.setText(getString(R.string.commit_changed_files, String.valueOf(filesList.size())));

          // update diff
          diffView.setText(change.diff);
        }, new ErrorActionBuilder()
						.add(new DefaultErrorAction(this, "failed to load git changes"))
						.build()));

		// setup commit button
		commitButton.setOnClickListener(v -> {
      showSpinner();
      String commitMessage = messageView.getText().toString();
      if ("".equals(commitMessage)) commitMessage = messageView.getHint().toString();

      uiUtils.showSpinner(spinnerContainerView, spinnerImageView);
      compositeSubscription.add(fileManager.commit(commitMessage)
          .compose(new DefaultTransformer<Void>())
          .subscribe(new Action1<Void>() {
            @Override
            public void call(Void nothing) {
              hideSpinner();
              Timber.d("commit success");
              setResult(RESULT_OK);
              Toast.makeText(CommitActivity.this, getString(R.string.commit_success), Toast.LENGTH_LONG).show();
              finish();
            }
          }, new ErrorActionBuilder()
              .add(new DefaultErrorAction(CommitActivity.this, "failed to commit"))
              .add(new HideSpinnerAction(CommitActivity.this))
              .build()));
    });

		// setup expand buttons
		changedFilesTitleView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleExpand(changedFilesExpandButton, changedFilesView);
			}
		});
		diffTitleView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleExpand(diffExpandButton, diffView);
			}
		});
		messageTitleView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleExpand(messageExpandButton, messageView);
			}
		});

		// restore expansions
		if (savedInstanceState != null) {
			if (savedInstanceState.getBoolean(STATE_FILES_EXPAND)) {
				changedFilesView.setVisibility(View.VISIBLE);
				changedFilesExpandButton.setBackgroundResource(R.drawable.ic_expand_less);
			}
			if (savedInstanceState.getBoolean(STATE_DIFF_EXPAND)) {
				diffView.setVisibility(View.VISIBLE);
				diffExpandButton.setBackgroundResource(R.drawable.ic_expand_less);
			}
			if (savedInstanceState.getBoolean(STATE_MESSAGE_EXPAND)) {
				messageView.setVisibility(View.VISIBLE);
				messageExpandButton.setBackgroundResource(R.drawable.ic_expand_less);
			}
		}
	}


	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(STATE_FILES_EXPAND, changedFilesView.getVisibility() != View.GONE);
		outState.putBoolean(STATE_DIFF_EXPAND, diffView.getVisibility() != View.GONE);
		outState.putBoolean(STATE_MESSAGE_EXPAND, messageView.getVisibility() != View.GONE);
	}


	private void toggleExpand(ImageButton button, View targetView) {
		if (targetView.getVisibility() == View.GONE) {
			expandView(targetView);
			button.setBackgroundResource(R.drawable.ic_expand_less);
		} else {
			collapseView(targetView);
			button.setBackgroundResource(R.drawable.ic_expand_more);
		}
	}


	/* Thanks to http://stackoverflow.com/a/13381228 */
	private void expandView(final View v) {
		v.measure(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		final int targetHeight = v.getMeasuredHeight();

		v.getLayoutParams().height = 0;
		v.setVisibility(View.VISIBLE);
		Animation a = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t) {
				v.getLayoutParams().height = interpolatedTime == 1
						? LayoutParams.WRAP_CONTENT
						: (int)(targetHeight * interpolatedTime);
				v.requestLayout();
			}

			@Override
			public boolean willChangeBounds() {
				return true;
			}
		};

		// 1dp/ms
		a.setDuration((int)(targetHeight / v.getContext().getResources().getDisplayMetrics().density));
		v.startAnimation(a);
	}


	private void collapseView(final View v) {
		final int initialHeight = v.getMeasuredHeight();

		Animation a = new Animation()
		{
			@Override
			protected void applyTransformation(float interpolatedTime, Transformation t) {
				if(interpolatedTime == 1){
					v.setVisibility(View.GONE);
				}else{
					v.getLayoutParams().height = initialHeight - (int)(initialHeight * interpolatedTime);
					v.requestLayout();
				}
			}

			@Override
			public boolean willChangeBounds() {
				return true;
			}
		};

		// 1dp/ms
		a.setDuration((int) (initialHeight / v.getContext().getResources().getDisplayMetrics().density));
		v.startAnimation(a);
	}


	private static class Change {

		private final Set<String> changedFiles, deletedFiles;
		private final String diff;

		public Change(Set<String> changedFiles, Set<String> deletedFiles, String diff) {
			this.changedFiles = changedFiles;
			this.deletedFiles = deletedFiles;
			this.diff = diff;
		}

	}

}

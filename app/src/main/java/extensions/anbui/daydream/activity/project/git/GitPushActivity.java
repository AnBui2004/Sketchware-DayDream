package extensions.anbui.daydream.activity.project.git;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

import extensions.anbui.daydream.configs.Configs;
import extensions.anbui.daydream.project.DRProjectTracker;
import extensions.anbui.daydream.ui.DialogUtils;
import extensions.anbui.daydream.file.FileUtils;
import extensions.anbui.daydream.git.GitUtils;
import pro.sketchware.R;
import pro.sketchware.databinding.ActivityDaydreamGitPushBinding;

public class GitPushActivity extends AppCompatActivity {

    private ActivityDaydreamGitPushBinding binding;
    private String projectID;
    private boolean pushProject = true;
    private boolean pushSourceCode = true;
    private boolean isDiff = false;
    private boolean isDiffChecked = false;

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().hasExtra("sc_id")) {
            projectID = getIntent().getStringExtra("sc_id");
        } else {
            finish();
            return;
        }
        EdgeToEdge.enable(this);
        binding = ActivityDaydreamGitPushBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);
        initialize();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!FileUtils.isFileExist(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + "/.git"))
            finish();
        else
            checkDiff();
    }

    private void initialize() {
        binding.swGradlefiles.setChecked(!FileUtils.isFileExist(FileUtils.getInternalStorageDir() + Configs.gitFolderDir + projectID + Configs.gitSourceFolderName + "build.gradle"));

        binding.lnGradlefiles.setOnClickListener(v -> binding.swGradlefiles.toggle());
        binding.lnLocallibraries.setOnClickListener(v -> binding.swLocallibraries.toggle());
        binding.lnCustomblocks.setOnClickListener(v -> binding.swCustomblocks.toggle());
        binding.lnApis.setOnClickListener(v -> binding.swApis.toggle());
        binding.btnDone.setOnClickListener(v -> pushProject());

        String[] options = new String[] {"All", "Project only", "Source code only"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                options
        );


        binding.filledExposedDropdown.setAdapter(adapter);

        binding.filledExposedDropdown.setOnItemClickListener((parent, view, position, id) -> {
            switch ((String) parent.getItemAtPosition(position)) {
                case "Project only":
                    pushProject = true;
                    pushSourceCode = false;
                    binding.lnGradlefiles.setVisibility(View.GONE);
                    binding.lnLocallibraries.setVisibility(View.VISIBLE);
                    binding.lnCustomblocks.setVisibility(View.VISIBLE);
                    break;
                case "Source code only":
                    pushProject = false;
                    pushSourceCode = true;
                    binding.lnGradlefiles.setVisibility(View.VISIBLE);
                    binding.lnLocallibraries.setVisibility(View.GONE);
                    binding.lnCustomblocks.setVisibility(View.GONE);
                    break;
                default:
                    pushProject = true;
                    pushSourceCode = true;
                    binding.lnGradlefiles.setVisibility(View.VISIBLE);
                    binding.lnLocallibraries.setVisibility(View.VISIBLE);
                    binding.lnCustomblocks.setVisibility(View.VISIBLE);
                    break;
            }
        });
    }

    private void checkDiff() {
        new Thread(() -> {
            isDiff = GitUtils.quickGetDiff(projectID);

            runOnUiThread(() -> {
                if (isDiff)
                    binding.cvNeedpull.setVisibility(View.VISIBLE);
                else
                    binding.cvNeedpull.setVisibility(View.GONE);

                isDiffChecked = true;
            });
        }).start();
    }

    private void pushProject() {
        if (isDiff || !isDiffChecked) {
            startPullProject();
        } else {
            startPushProject();
        }
    }

    private void startPushProject() {
        if (pushSourceCode) {
            if (!DRProjectTracker.isAllowBuildNow(this)) return;
        }

        View progressView = LayoutInflater.from(this).inflate(R.layout.progress_msg_box, null);
        TextView progress_text = progressView.findViewById(R.id.tv_progress);
        progress_text.setText("Pushing...");
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this)
                .setView(progressView)
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            boolean result = GitUtils.push(this, projectID, progress_text, Objects.requireNonNull(binding.edTitle.getText()).toString(),  Objects.requireNonNull(binding.edDescription.getText()).toString(), binding.swLocallibraries.isChecked(), binding.swCustomblocks.isChecked(), binding.swApis.isChecked(), pushProject, pushSourceCode, binding.swKeepoldfiles.isChecked(), binding.swGradlefiles.isChecked());

            runOnUiThread(() -> {
                if (result) {
                    progressDialog.dismiss();
                    DialogUtils.oneDialog(this,
                            "Done",
                            "Pushed to your GitHub repository.",
                            "OK",
                            true,
                            R.drawable.ic_mtrl_check,
                            true, this::finish, this::finish);
                } else {
                    progressDialog.dismiss();
                    DialogUtils.oneDialog(this,
                            "Error",
                            "There was an error pushing to your GitHub repository. Please try again later.",
                            "OK",
                            true,
                            R.drawable.ic_mtrl_warning,
                            true, null, null);
                }
                checkDiff();
            });
        }).start();
    }

    private void startPullProject() {
        View progressView = LayoutInflater.from(this).inflate(R.layout.progress_msg_box, null);
        TextView progress_text = progressView.findViewById(R.id.tv_progress);
        progress_text.setText("Pulling...");
        AlertDialog progressDialog = new MaterialAlertDialogBuilder(this)
                .setView(progressView)
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            boolean result = GitUtils.pull(projectID);

            runOnUiThread(() -> {
                progressDialog.dismiss();
                if (result) {
                    startPushProject();
                } else {
                    DialogUtils.oneDialog(this,
                            "Error",
                            "Unable to pull from your GitHub repository. Please try again later.",
                            "OK",
                            true,
                            R.drawable.ic_mtrl_warning,
                            true, null, null);
                }
                checkDiff();
            });
        }).start();
    }
}
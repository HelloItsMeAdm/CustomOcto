package com.helloitsmeadm.customocto;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Html;
import android.text.Spanned;
import android.text.TextUtils;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.ocpsoft.prettytime.PrettyTime;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@SuppressLint("SetTextI18n")
public class MainActivity extends AppCompatActivity {
    ScrollView root;
    TextView loading_root;

    // Navbar
    ImageButton requestRefresh;
    TextView mainTitle;
    FloatingActionButton lockControls;

    // Status box
    TextView title;
    TextView subtitle;
    ProgressBar progressbar;
    TextView progressText;
    TextView progressCurrent;
    TextView progressLeft;

    // File info
    TextView fileValue;
    TextView willEndAtValue;
    TextView totalPrintTimeValue;
    TextView uploadedValue;
    TextView filamentValue;

    // Temperature box
    TextView tableHotendActual;
    TextView tableHotendTarget;
    TextView tableBedActual;
    TextView tableBedTarget;

    // Controls
    ImageButton control_connect;
    ImageButton control_disconnect;
    ImageButton control_restart;
    ImageButton control_psu;
    Button control_settemp;
    Button control_changefilament;
    Button control_cancel;
    Button control_pauseresume;
    Button control_database;

    // Files
    View filesView;
    LinearLayout filesList;
    TextView loadingFiles;

    // Logs
    TextView titleLogs;
    ImageButton clearLogs;
    TextView showLogs;

    // Webcam
    View webcamBackground;
    TextView titleWebcam;
    TextView webcamUpdate;
    RelativeLayout fullWebcam;
    ImageView webcam;
    TextView loadingWebcam;
    ImageButton refreshWebcamButton;
    ImageButton fullScreenButton;

    // Values
    String titleV = "Offline";
    String subtitleV = "Not connected to printer";
    double progressBarV = 0.0;
    String progressTextV = "0 %";
    String progressCurrentV = "0 seconds";
    String progressLeftV = "0 seconds";

    String fileValueV = "No file selected";
    Spanned willEndAtValueV = Html.fromHtml("Currently not printing.");
    String totalPrintTimeValueV = "No file selected";
    String uploadedValueV = "No file selected";
    String filamentValueV = "No file selected";

    double tableHotendActualV = 0.0;
    double tableHotendTargetV = 0.0;
    double tableBedActualV = 0.0;
    double tableBedTargetV = 0.0;

    String webcamUpdateV = "Loading...";

    // Etc
    static FirebaseFirestore db = FirebaseFirestore.getInstance();
    Thread thread;
    ArrayList<String> tempNames = new ArrayList<>();
    ArrayList<Integer> tempHotend = new ArrayList<>();
    ArrayList<Integer> tempBed = new ArrayList<>();
    boolean connected = false;
    boolean controlsActive = false;
    boolean paused = false;
    boolean webcamRefresh = false;
    long refreshWebcamCntTime = 5000;
    long totalCntTime = 2500;
    long pausedCntTime = 3000;
    static Bitmap webcamImage;
    boolean logsHidden = false;
    boolean fileValueFull = false;
    ArrayList<Long> usedNotifications = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        hideControls(Html.fromHtml(""), true);
        toggleService();

        if (getWifiState() == 0) {
            hideControls(Html.fromHtml("<b>No internet!</b><br><br><Small>Connect to the internet to use this app.</Small>"), true);

            // Every second check if the user has connected to the internet
            CountDownTimer internetCnt = new CountDownTimer(1000, 1000) {
                @Override
                public void onTick(long l) {

                }

                @Override
                public void onFinish() {
                    if (getWifiState() == 0) {
                        start();
                    } else {
                        loginToFirebase();
                    }
                }
            };
            internetCnt.start();
        } else {
            loginToFirebase();
        }
    }

    private void getWidgets() {
        requestRefresh = findViewById(R.id.requestRefresh);
        mainTitle = findViewById(R.id.mainTitle);
        mainTitle.setText(Html.fromHtml("Custom<b>Octo</b>"));

        title = findViewById(R.id.title);
        subtitle = findViewById(R.id.subtitle);
        progressbar = findViewById(R.id.progressbar);
        progressText = findViewById(R.id.progressText);
        progressCurrent = findViewById(R.id.progressCurrent);
        progressLeft = findViewById(R.id.progressLeft);

        fileValue = findViewById(R.id.fileValue);
        willEndAtValue = findViewById(R.id.willEndAtValue);
        totalPrintTimeValue = findViewById(R.id.totalPrintTimeValue);
        uploadedValue = findViewById(R.id.uploadedValue);
        filamentValue = findViewById(R.id.filamentValue);

        tableHotendActual = findViewById(R.id.tableHotendActual);
        tableHotendTarget = findViewById(R.id.tableHotendTarget);
        tableBedActual = findViewById(R.id.tableBedActual);
        tableBedTarget = findViewById(R.id.tableBedTarget);

        control_connect = findViewById(R.id.control_connect);
        control_disconnect = findViewById(R.id.control_disconnect);
        control_settemp = findViewById(R.id.control_settemp);
        control_restart = findViewById(R.id.control_restart);
        control_psu = findViewById(R.id.control_psu);
        control_changefilament = findViewById(R.id.control_changefilament);
        control_cancel = findViewById(R.id.control_cancel);
        control_pauseresume = findViewById(R.id.control_pauseresume);
        control_database = findViewById(R.id.control_database);

        filesView = findViewById(R.id.filesView);
        loadingFiles = findViewById(R.id.loadingFiles);
        filesList = findViewById(R.id.filesList);

        titleLogs = findViewById(R.id.titleLogs);
        clearLogs = findViewById(R.id.clearLogs);
        showLogs = findViewById(R.id.showLogs);

        webcamBackground = findViewById(R.id.webcamBackground);
        titleWebcam = findViewById(R.id.titleWebcam);
        webcamUpdate = findViewById(R.id.webcamUpdate);
        fullWebcam = findViewById(R.id.fullWebcam);
        webcam = findViewById(R.id.webcam);
        loadingWebcam = findViewById(R.id.loadingWebcam);
        refreshWebcamButton = findViewById(R.id.refreshWebcamButton);
        fullScreenButton = findViewById(R.id.fullScreenButton);

        titleWebcam.setOnClickListener(view -> {
            if (!webcamRefresh) {
                fullWebcam.setVisibility(View.VISIBLE);
                refreshWebcam(webcam, loadingWebcam);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) webcamBackground.getLayoutParams();
                params.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.fullWebcam);
                webcamUpdate.setVisibility(View.VISIBLE);
                webcamUpdate.setText(webcamUpdateV);
                webcamBackground.setLayoutParams(params);
                webcamRefresh = true;
            } else {
                fullWebcam.setVisibility(View.GONE);
                RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) webcamBackground.getLayoutParams();
                params.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.titleWebcam);
                webcamUpdate.setVisibility(View.INVISIBLE);
                webcamBackground.setLayoutParams(params);
                webcamRefresh = false;
            }
        });

        refreshWebcamButton.setOnClickListener(v -> {
            JSONArray commandList = new JSONArray();
            commandList.put("refreshWebcam");
            sendCommand(commandList, this);
            ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_hourglass_bottom_24, "Refresh of webcam requested!");
        });

        fullScreenButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, FullscreenWebcam.class);
            startActivity(intent);
        });

        control_settemp.setOnClickListener(view -> {
            if (!controlsActive) {
                ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_lock_24, "Controls disabled!");
            }
        });

        control_psu.setOnClickListener(view -> {
            if (controlsActive) {
                JSONArray commandList = new JSONArray();
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Confirm command");
                builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
                if (connected) {
                    builder.setMessage(Html.fromHtml("Are you sure you want to send the command?" + "<br><br><b>" + "• Turn PSU Off" + "</b>" + " (turnPSUOff)"));
                    builder.setPositiveButton("Yes", (dialog1, which) -> {
                        commandList.put("turnPSUOff");
                        sendCommand(commandList, this);
                    });
                } else {
                    builder.setMessage(Html.fromHtml("Are you sure you want to send the command?" + "<br><br><b>" + "• Turn PSU On" + "</b>" + " (turnPSUOn)"));
                    builder.setPositiveButton("Yes", (dialog1, which) -> {
                        commandList.put("turnPSUOn");
                        sendCommand(commandList, this);
                    });
                }
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_lock_24, "Controls disabled!");
            }
        });

        control_changefilament.setOnClickListener(view -> {
            if (controlsActive) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Confirm command");
                builder.setMessage(Html.fromHtml("Are you sure you want to send the command?" + "<br><br><b>" + "• Change Filament" + "</b>" + " (M600)"));
                builder.setPositiveButton("Yes", (dialog, which) -> {
                    JSONArray commandList = new JSONArray();
                    commandList.put("M600");
                    sendCommand(commandList, this);
                });
                builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_lock_24, "Controls disabled!");
            }
        });

        control_connect.setOnClickListener(view -> {
            if (controlsActive) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Confirm command");
                builder.setMessage(Html.fromHtml("Are you sure you want to send the command?" + "<br><br><b>" + "• Try to connect" + "</b>" + " (connect)"));
                builder.setPositiveButton("Yes", (dialog, which) -> {
                    JSONArray commandList = new JSONArray();
                    commandList.put("connect");
                    sendCommand(commandList, this);
                });
                builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_lock_24, "Controls disabled!");
            }
        });

        control_disconnect.setOnClickListener(view -> {
            if (controlsActive) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Confirm command");
                builder.setMessage(Html.fromHtml("Are you sure you want to send the command?" + "<br><br><b>" + "• Try to disconnect" + "</b>" + " (disconnect)"));
                builder.setPositiveButton("Yes", (dialog, which) -> {
                    JSONArray commandList = new JSONArray();
                    commandList.put("disconnect");
                    sendCommand(commandList, this);
                });
                builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_lock_24, "Controls disabled!");
            }
        });
        requestRefresh.setOnClickListener(view -> {
            JSONArray commandList = new JSONArray();
            commandList.put("refresh");
            sendCommand(commandList, this);
        });

        control_database.setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, DatabaseActivity.class);
            startActivity(intent);
        });

        control_restart.setOnClickListener(view -> {
            if (controlsActive) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Confirm command");
                builder.setMessage(Html.fromHtml("Are you sure you want to send the command?" + "<br><br><b>" + "• Restart OctoPrint" + "</b>" + " (restart)"));
                builder.setPositiveButton("Yes", (dialog, which) -> {
                    JSONArray commandList = new JSONArray();
                    commandList.put("restart");
                    sendCommand(commandList, this);
                });
                builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_lock_24, "Controls disabled!");
            }
        });

        titleLogs.setOnClickListener(view -> {
            if (logsHidden) {
                logsHidden = false;
                showLogs.setText("Loading...");
                ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_hourglass_bottom_24, "Loading logs...");
            } else {
                logsHidden = true;
                showLogs.setText("Logs are hidden.");
                ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_visibility_off_24, "Logs are now hidden.");
            }
        });

        clearLogs.setOnClickListener(view -> {
            if (controlsActive) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Confirm command");
                builder.setMessage(Html.fromHtml("Are you sure you want to clear the logs?"));
                builder.setPositiveButton("Yes", (dialog, which) -> {
                    JSONArray commandList = new JSONArray();
                    commandList.put("clearLogs");
                    sendCommand(commandList, this);
                });
                builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_lock_24, "Controls disabled!");
            }
        });

        fileValue.setOnClickListener(view -> {
            if (fileValueFull) {
                fileValueFull = false;
                fileValue.setMaxLines(1);
            } else {
                fileValueFull = true;
                fileValue.setMaxLines(Integer.MAX_VALUE);
            }
        });
    }

    private void sharedPreferences() {
        SharedPreferences sharedPreferences = getSharedPreferences("sharedPrefs", MODE_PRIVATE);
        controlsActive = sharedPreferences.getBoolean("controlsActive", false);

        if (controlsActive) {
            lockControls.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.green)));
            lockControls.setImageResource(R.drawable.ic_baseline_lock_open_24);
        } else {
            lockControls.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.red)));
            lockControls.setImageResource(R.drawable.ic_baseline_lock_24);
        }

        lockControls.setOnClickListener(view -> {
            if (controlsActive) {
                lockControls.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.red)));
                lockControls.setImageResource(R.drawable.ic_baseline_lock_24);
                controlsActive = false;
            } else {
                lockControls.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.green)));
                lockControls.setImageResource(R.drawable.ic_baseline_lock_open_24);
                controlsActive = true;
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putBoolean("controlsActive", controlsActive);
            editor.apply();
        });
    }

    private void jobCommands() {
        if (!paused) {
            control_pauseresume.setText("Pause");
            control_pauseresume.setOnClickListener(view -> {
                if (controlsActive) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Confirm command");
                    builder.setMessage(Html.fromHtml("Are you sure you want to send the command?" + "<br><br><b>" + "• Pause current job" + "</b>" + " (pause)"));
                    builder.setPositiveButton("Yes", (dialog, which) -> {
                        JSONArray commandList = new JSONArray();
                        commandList.put("pause");
                        sendCommand(commandList, this);
                    });
                    builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_lock_24, "Controls disabled!");
                }
            });
        } else {
            control_pauseresume.setText("Resume");
            control_pauseresume.setOnClickListener(view -> {
                if (controlsActive) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setTitle("Confirm command");
                    builder.setMessage(Html.fromHtml("Are you sure you want to send the command?" + "<br><br><b>" + "• Resume current job" + "</b>" + " (resume)"));
                    builder.setPositiveButton("Yes", (dialog, which) -> {
                        JSONArray commandList = new JSONArray();
                        commandList.put("resume");
                        sendCommand(commandList, this);
                    });
                    builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
                    AlertDialog dialog = builder.create();
                    dialog.show();
                } else {
                    ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_lock_24, "Controls disabled!");
                }
            });
        }
        control_cancel.setOnClickListener(view -> {
            if (controlsActive) {
                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("Confirm command");
                builder.setMessage(Html.fromHtml("Are you sure you want to send the command?" + "<br><br><b>" + "• Cancel current job" + "</b>" + " (cancel)"));
                builder.setPositiveButton("Yes", (dialog, which) -> {
                    JSONArray commandList = new JSONArray();
                    commandList.put("cancel");
                    sendCommand(commandList, this);
                });
                builder.setNegativeButton("No", (dialog, which) -> dialog.dismiss());
                AlertDialog dialog = builder.create();
                dialog.show();
            } else {
                ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_lock_24, "Controls disabled!");
            }
        });
    }

    private void update(boolean firstTime) {
        thread = new Thread(() -> db.collection("json").document("status").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                try {
                    JSONObject data = new JSONObject(Objects.requireNonNull(Objects.requireNonNull(task.getResult()).getData()));
                    JSONObject printerJson = data.getJSONObject("printer");
                    JSONObject jobJson = data.getJSONObject("job");
                    JSONArray tempJson = data.getJSONArray("temperature");
                    JSONObject filesJson = data.getJSONObject("files");

                    loadingFiles.setVisibility(View.GONE);
                    filesList.removeAllViews();
                    RelativeLayout.LayoutParams filesViewParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                    filesViewParams.addRule(RelativeLayout.ALIGN_TOP, R.id.titleFiles);
                    filesViewParams.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.filesList);
                    filesView.setLayoutParams(filesViewParams);

                    int folderint = 0;
                    int childrenint = 0;
                    for (int i = 0; i < filesJson.getJSONArray("files").length(); i++) {
                        folderint++;
                        JSONObject category = filesJson.getJSONArray("files").getJSONObject(i);
                        JSONArray children = category.getJSONArray("children");

                        LinearLayout categoryLayout = new LinearLayout(MainActivity.this);
                        categoryLayout.setOrientation(LinearLayout.VERTICAL);
                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        params.setMargins(0, 0, 0, 25);
                        categoryLayout.setLayoutParams(params);

                        if (folderint != 1) {
                            View line = new View(MainActivity.this);
                            LinearLayout.LayoutParams lineParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 15);
                            lineParams.setMargins(20, 0, 20, 30);
                            line.setLayoutParams(lineParams);
                            line.setBackground(getDrawable(R.drawable.rounded_corners));
                            line.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
                            categoryLayout.addView(line);
                        }

                        TextView folder = new TextView(MainActivity.this);
                        folder.setText(Html.fromHtml("<b>• " + category.getString("display") + "</b> (" + convertSize(category.getInt("size")) + ")"));
                        folder.setTextSize(22);
                        folder.setTextColor(Color.WHITE);
                        folder.setTypeface(ResourcesCompat.getFont(MainActivity.this, R.font.manrope));
                        LinearLayout.LayoutParams folderParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                        folderParams.setMargins(0, 0, 0, 20);
                        folder.setLayoutParams(folderParams);
                        categoryLayout.addView(folder);

                        for (int j = 0; j < children.length(); j++) {
                            childrenint++;
                            LinearLayout fileLayout = new LinearLayout(MainActivity.this);
                            fileLayout.setOrientation(LinearLayout.VERTICAL);
                            LinearLayout.LayoutParams fileParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                            fileParams.setMargins(0, 0, 0, 20);
                            fileLayout.setLayoutParams(fileParams);

                            String path = children.getJSONObject(j).getString("path");
                            String name = children.getJSONObject(j).getString("name");
                            fileLayout.setOnClickListener(view -> {
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                                builder.setTitle("File operation");
                                builder.setMessage(Html.fromHtml("What do you want to do with this file?" + "<br><br><b>" + "• " + name + "</b>"));
                                builder.setNegativeButton("Load", (dialog, which) -> {
                                    JSONArray commandList = new JSONArray();
                                    commandList.put("?load " + path);
                                    sendCommand(commandList, this);
                                });
                                builder.setPositiveButton("Print", (dialog, which) -> {
                                    JSONArray commandList = new JSONArray();
                                    commandList.put("?print " + path);
                                    sendCommand(commandList, this);
                                });
                                builder.setNeutralButton("Delete", (dialog, which) -> {
                                    JSONArray commandList = new JSONArray();
                                    commandList.put("?delete " + path);
                                    sendCommand(commandList, this);
                                });
                                AlertDialog dialog = builder.create();
                                if (controlsActive) {
                                    dialog.show();
                                    if (!connected) {
                                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setEnabled(false);
                                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
                                    }
                                } else {
                                    ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_lock_24, "Controls disabled!");
                                }
                            });

                            TextView fileView = new TextView(MainActivity.this);
                            fileView.setText("└ " + name);
                            fileView.setTextSize(19);
                            int fileTextColor = R.color.white;
                            if (children.getJSONObject(j).has("prints")) {
                                if (children.getJSONObject(j).getJSONObject("prints").getJSONObject("last").getBoolean("success")) {
                                    fileTextColor = R.color.green;
                                } else {
                                    fileTextColor = R.color.red;
                                }
                            }
                            fileView.setTextColor(getResources().getColor(fileTextColor));
                            fileView.setMaxLines(1);
                            fileView.setEllipsize(TextUtils.TruncateAt.END);
                            fileView.setTypeface(ResourcesCompat.getFont(MainActivity.this, R.font.manrope));
                            fileLayout.addView(fileView);

                            TextView fileSize = new TextView(MainActivity.this);
                            fileSize.setText(Html.fromHtml("        <b>• Size:</b> " + convertSize(children.getJSONObject(j).getInt("size"))));
                            fileSize.setTextSize(15);
                            fileSize.setTextColor(getResources().getColor(fileTextColor));
                            fileSize.setTypeface(ResourcesCompat.getFont(MainActivity.this, R.font.manrope));
                            fileLayout.addView(fileSize);

                            Date dateObject = new Date(children.getJSONObject(j).getInt("date") * 1000L);
                            PrettyTime prettyTime = new PrettyTime();

                            TextView fileDate = new TextView(MainActivity.this);
                            fileDate.setText(Html.fromHtml("        <b>• Uploaded:</b> " + prettyTime.format(dateObject)));
                            fileDate.setTextSize(15);
                            fileDate.setTextColor(getResources().getColor(fileTextColor));
                            fileDate.setTypeface(ResourcesCompat.getFont(MainActivity.this, R.font.manrope));
                            fileLayout.addView(fileDate);

                            String[] pathSplited = children.getJSONObject(j).getString("path").split("_");
                            String estimatedPrintTimeSimple = pathSplited[pathSplited.length - 1].split("\\.")[0].replaceAll("h", "h ");

                            TextView estimatedPrintTimeText = new TextView(MainActivity.this);
                            estimatedPrintTimeText.setText(Html.fromHtml("        <b>• Estimated print time:</b> " + estimatedPrintTimeSimple));
                            estimatedPrintTimeText.setTextSize(15);
                            estimatedPrintTimeText.setTextColor(getResources().getColor(fileTextColor));
                            estimatedPrintTimeText.setTypeface(ResourcesCompat.getFont(MainActivity.this, R.font.manrope));
                            fileLayout.addView(estimatedPrintTimeText);

                            String filamentLengthSimple = "Not available";
                            if (children.getJSONObject(j).has("gcodeAnalysis") && children.getJSONObject(j).getJSONObject("gcodeAnalysis").getJSONObject("filament").has("tool0")) {
                                filamentLengthSimple = round(children.getJSONObject(j).getJSONObject("gcodeAnalysis").getJSONObject("filament").getJSONObject("tool0").getDouble("length") / 1000, 1) + "m";
                            }
                            TextView filamentLengthText = new TextView(MainActivity.this);
                            filamentLengthText.setText(Html.fromHtml("        <b>• Filament length:</b> " + filamentLengthSimple));
                            filamentLengthText.setTextSize(15);
                            filamentLengthText.setTextColor(getResources().getColor(fileTextColor));
                            filamentLengthText.setTypeface(ResourcesCompat.getFont(MainActivity.this, R.font.manrope));
                            fileLayout.addView(filamentLengthText);

                            if (children.length() > 1 && j != children.length() - 1) {
                                View linechildren = new View(MainActivity.this);
                                LinearLayout.LayoutParams lineParamschildren = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 5);
                                lineParamschildren.setMargins(20, 30, 20, 15);
                                linechildren.setLayoutParams(lineParamschildren);
                                linechildren.setBackground(getDrawable(R.drawable.rounded_corners));
                                linechildren.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.white)));
                                fileLayout.addView(linechildren);
                            }
                            categoryLayout.addView(fileLayout);
                        }
                        filesList.addView(categoryLayout);
                    }

                    if (folderint == 0 && childrenint == 0) {
                        TextView noFiles = new TextView(MainActivity.this);
                        noFiles.setText("No files found.");
                        noFiles.setTextSize(20);
                        noFiles.setTextColor(getResources().getColor(R.color.white));
                        noFiles.setTypeface(ResourcesCompat.getFont(MainActivity.this, R.font.manrope));
                        RelativeLayout.LayoutParams noFilesParams = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
                        noFilesParams.addRule(RelativeLayout.CENTER_HORIZONTAL);
                        noFilesParams.topMargin = 20;
                        noFilesParams.bottomMargin = 80;
                        noFiles.setLayoutParams(noFilesParams);
                        filesList.setGravity(Gravity.CENTER_HORIZONTAL);
                        filesList.addView(noFiles);
                    }

                    tempNames.clear();
                    tempHotend.clear();
                    tempBed.clear();

                    tempNames.add("Off");
                    tempHotend.add(0);
                    tempBed.add(0);

                    for (int i = 0; i < tempJson.length(); i++) {
                        JSONObject json_data = tempJson.getJSONObject(i);
                        String name = json_data.getString("name");
                        tempNames.add(name);
                        int hotend = json_data.getInt("extruder");
                        tempHotend.add(hotend);
                        int bed = json_data.getInt("bed");
                        tempBed.add(bed);
                    }

                    ArrayAdapter<String> mArrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_expandable_list_item_1, tempNames);

                    control_settemp.setOnClickListener(view -> {
                        if (controlsActive) {
                            new AlertDialog.Builder(MainActivity.this).setAdapter(mArrayAdapter, (dialog, which) -> setTemp(which)).setNegativeButton("Cancel", null).setTitle("Set temperature").show();
                        } else {
                            ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_lock_24, "Controls disabled!");
                        }
                    });

                    if (printerJson.has("error")) {
                        // ClosedOrError
                        titleV = "Offline";
                        subtitleV = "Not connected to printer";
                        progressBarV = 0.0;
                        progressTextV = "0 %";
                        progressCurrentV = "0 seconds";
                        progressLeftV = "0 seconds";

                        fileValueV = "No file selected";
                        willEndAtValueV = Html.fromHtml("Currently not printing.");
                        totalPrintTimeValueV = "No file selected";
                        uploadedValueV = "No file selected";
                        filamentValueV = "No file selected";

                        tableHotendActualV = 0;
                        tableHotendTargetV = 0;
                        tableBedActualV = 0;
                        tableBedTargetV = 0;
                        connected = false;
                        Notifications.removeProgressNotification(MainActivity.this);
                    } else if (jobJson.getString("state").equals("Operational")) {
                        tableHotendActualV = round(printerJson.getJSONObject("temperature").getJSONObject("tool0").getDouble("actual"), 1);
                        tableHotendTargetV = round(printerJson.getJSONObject("temperature").getJSONObject("tool0").getDouble("target"), 1);
                        tableBedActualV = round(printerJson.getJSONObject("temperature").getJSONObject("bed").getDouble("actual"), 1);
                        tableBedTargetV = round(printerJson.getJSONObject("temperature").getJSONObject("bed").getDouble("target"), 1);
                        connected = true;
                        Notifications.removeProgressNotification(MainActivity.this);

                        if (jobJson.getJSONObject("job").getJSONObject("file").getString("display").equals("null")) {
                            // Operational wihout file loaded
                            titleV = "Idle";
                            subtitleV = "Printer is ready";
                            progressBarV = 0.0;
                            progressTextV = "0 %";
                            progressCurrentV = "0 seconds";
                            progressLeftV = "0 seconds";

                            fileValueV = "No file selected";
                            willEndAtValueV = Html.fromHtml("Currently not printing.");
                            totalPrintTimeValueV = "No file selected";
                            uploadedValueV = "No file selected";
                            filamentValueV = "No file selected";
                        } else {
                            // Operational with file loaded
                            titleV = "File loaded";
                            progressBarV = 0.0;
                            progressTextV = "0 %";
                            progressCurrentV = "0 seconds";

                            long uploadedRaw = jobJson.getJSONObject("job").getJSONObject("file").getLong("date");
                            String uploaded = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ENGLISH).format(new Date(uploadedRaw * 1000));

                            long totalPrintTimeValueVRaw = jobJson.getJSONObject("job").getLong("estimatedPrintTime");
                            if (totalPrintTimeValueVRaw > 3600) {
                                totalPrintTimeValueV = String.format(Locale.ENGLISH, "%d hours", totalPrintTimeValueVRaw / 3600);
                            } else if (totalPrintTimeValueVRaw > 60) {
                                totalPrintTimeValueV = String.format(Locale.ENGLISH, "%d minutes", totalPrintTimeValueVRaw / 60);
                            } else if (totalPrintTimeValueVRaw > 0) {
                                totalPrintTimeValueV = String.format(Locale.ENGLISH, "%d seconds", totalPrintTimeValueVRaw);
                            }

                            String path = jobJson.getJSONObject("job").getJSONObject("file").getString("path");

                            String[] underscores = path.split("_");
                            String pathColor = underscores[0].split("/")[0];
                            String filamentColor = pathColor.substring(0, 1).toUpperCase() + pathColor.substring(1).toLowerCase();
                            String filamentMaterial = underscores[underscores.length - 3];

                            int index = 0;
                            for (int i = 0; i < underscores.length; i++) {
                                if (underscores[i].contains("/")) {
                                    index = i;
                                    break;
                                }
                            }
                            subtitleV = underscores[index].split("/")[1] + " " + String.join(" ", Arrays.copyOfRange(underscores, 1 + index, underscores.length - 4));

                            progressLeftV = totalPrintTimeValueV;

                            fileValueV = jobJson.getJSONObject("job").getJSONObject("file").getString("display");

                            Calendar endAt = Calendar.getInstance();

                            try {
                                if (jobJson.getJSONObject("progress").getLong("printTimeLeft") == 0) {
                                    willEndAtValueV = Html.fromHtml("Print done!");
                                } else {
                                    endAt.setTimeInMillis(System.currentTimeMillis() + (jobJson.getJSONObject("progress").getLong("printTimeLeft") * 1000));
                                    willEndAtValueV = Html.fromHtml("Will end at <b>" + new SimpleDateFormat("HH:mm", Locale.ENGLISH).format(endAt.getTime()) + "</b>");
                                }
                            } catch (Exception e) {
                                endAt.setTimeInMillis(System.currentTimeMillis() + (jobJson.getJSONObject("job").getLong("estimatedPrintTime") * 1000));
                                willEndAtValueV = Html.fromHtml("Will end at <b>" + new SimpleDateFormat("HH:mm", Locale.ENGLISH).format(endAt.getTime()) + "</b> <i>(Not acurate)</i>");
                            }

                            totalPrintTimeValueV = underscores[underscores.length - 1].split("\\.")[0];
                            uploadedValueV = uploaded;
                            filamentValueV = filamentColor + " (" + filamentMaterial + ")";
                        }
                    } else if (jobJson.getString("state").equals("Printing") || jobJson.getString("state").equals("Paused") || paused) {
                        // Printing or paused
                        if (paused) {
                            titleV = "Paused";
                        } else {
                            titleV = jobJson.getString("state");
                        }

                        String path = jobJson.getJSONObject("job").getJSONObject("file").getString("path");

                        progressBarV = jobJson.getJSONObject("progress").getDouble("completion");

                        progressTextV = Math.round(jobJson.getJSONObject("progress").getDouble("completion")) + " %";

                        long progressCurrentVRaw = jobJson.getJSONObject("progress").getLong("printTime");
                        if (progressCurrentVRaw > 3600) {
                            progressCurrentV = String.format(Locale.ENGLISH, "%d hours", progressCurrentVRaw / 3600);
                        } else if (progressCurrentVRaw > 60) {
                            progressCurrentV = String.format(Locale.ENGLISH, "%d minutes", progressCurrentVRaw / 60);
                        } else if (progressCurrentVRaw > 0) {
                            progressCurrentV = String.format(Locale.ENGLISH, "%d seconds", progressCurrentVRaw);
                        }

                        long progressLeftVRaw = jobJson.getJSONObject("progress").getLong("printTimeLeft");
                        if (progressLeftVRaw > 3600) {
                            progressLeftV = String.format(Locale.ENGLISH, "%d hours", progressLeftVRaw / 3600);
                        } else if (progressLeftVRaw > 60) {
                            progressLeftV = String.format(Locale.ENGLISH, "%d minutes", progressLeftVRaw / 60);
                        } else if (progressLeftVRaw > 0) {
                            progressLeftV = String.format(Locale.ENGLISH, "%d seconds", progressLeftVRaw);
                        }

                        fileValueV = jobJson.getJSONObject("job").getJSONObject("file").getString("display");

                        long uploadedRaw = jobJson.getJSONObject("job").getJSONObject("file").getLong("date");
                        uploadedValueV = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.ENGLISH).format(new Date(uploadedRaw * 1000));

                        String[] underscores = path.split("_");
                        String pathColor = underscores[0].split("/")[0];
                        String filamentColor = pathColor.substring(0, 1).toUpperCase() + pathColor.substring(1).toLowerCase();
                        String filamentMaterial = underscores[underscores.length - 3];

                        int index = 0;
                        for (int i = 0; i < underscores.length; i++) {
                            if (underscores[i].contains("/")) {
                                index = i;
                                break;
                            }
                        }
                        subtitleV = underscores[index].split("/")[1] + " " + String.join(" ", Arrays.copyOfRange(underscores, 1 + index, underscores.length - 4));

                        filamentValueV = filamentColor + " (" + filamentMaterial + ")";
                        totalPrintTimeValueV = underscores[underscores.length - 1].split("\\.")[0];

                        Calendar endAt = Calendar.getInstance();
                        endAt.setTimeInMillis(System.currentTimeMillis() + (progressLeftVRaw * 1000));
                        String endAtTime = new SimpleDateFormat("HH:mm", Locale.ENGLISH).format(endAt.getTime());
                        willEndAtValueV = Html.fromHtml("Will end at <b>" + endAtTime + "</b>");

                        tableHotendActualV = round(printerJson.getJSONObject("temperature").getJSONObject("tool0").getDouble("actual"), 1);
                        tableHotendTargetV = round(printerJson.getJSONObject("temperature").getJSONObject("tool0").getDouble("target"), 1);
                        tableBedActualV = round(printerJson.getJSONObject("temperature").getJSONObject("bed").getDouble("actual"), 1);
                        tableBedTargetV = round(printerJson.getJSONObject("temperature").getJSONObject("bed").getDouble("target"), 1);
                        connected = true;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            Notifications.showProgressNotification(MainActivity.this, "Printed " + Math.round(progressBarV) + "%", (int) progressBarV, webcamImage, subtitleV, endAtTime);
                        }
                    } else {
                        // Unknown
                        titleV = "Error";
                        subtitleV = "Unknown state";
                        progressBarV = 0.0;
                        progressTextV = "0 %";
                        progressCurrentV = "0 seconds";
                        progressLeftV = "0 seconds";

                        fileValueV = "No file selected";
                        willEndAtValueV = Html.fromHtml("Currently not printing.");
                        totalPrintTimeValueV = "No file selected";
                        uploadedValueV = "No file selected";
                        filamentValueV = "No file selected";

                        tableHotendActualV = 0.0;
                        tableHotendTargetV = 0.0;
                        tableBedActualV = 0.0;
                        tableBedTargetV = 0.0;
                        connected = false;
                        Notifications.removeProgressNotification(MainActivity.this);
                    }
                    if (firstTime) {
                        root.setVisibility(View.VISIBLE);
                        lockControls.setVisibility(View.VISIBLE);
                        loading_root.setVisibility(View.GONE);
                    }
                    jobCommands();
                    updateWidgets();
                } catch (JSONException e) {
                    e.printStackTrace();
                    root.setVisibility(View.VISIBLE);
                    lockControls.setVisibility(View.VISIBLE);
                    loading_root.setVisibility(View.GONE);
                    title.setText("Error");
                    subtitle.setMaxLines(10);
                    subtitle.setText(e.getMessage());
                }
            } else {
                root.setVisibility(View.VISIBLE);
                lockControls.setVisibility(View.VISIBLE);
                loading_root.setVisibility(View.GONE);
                title.setText("Error");
                subtitle.setMaxLines(10);
                subtitle.setText(task.getException().getMessage());
            }
        }));
        thread.start();
    }

    private void setTemp(int index) {
        JSONArray commandList = new JSONArray();
        commandList.put("M140 S" + tempBed.get(index));
        commandList.put("M109 S" + tempHotend.get(index));
        sendCommand(commandList, this);
    }

    public static void sendCommand(JSONArray commandList, Context context) {
        db.collection("json").document("commands").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    ArrayList<String> old = (ArrayList<String>) document.get("list");
                    for (int i = 0; i < commandList.length(); i++) {
                        try {
                            Objects.requireNonNull(old).add(commandList.getString(i));
                            if (commandList.getString(i).equals("refresh")) {
                                ToastManager.run((Activity) context, 3, R.drawable.ic_baseline_hourglass_bottom_24, "Refresh requested!");
                            } else if (commandList.getString(i).equals("delete") || commandList.getString(i).equals("disconnect")) {
                                Objects.requireNonNull(old).add("refresh");
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    db.collection("json").document("commands").update("list", old);
                }
            }
        });
    }

    private void updateWidgets() {
        title.setText(titleV);
        subtitle.setText(subtitleV);
        progressbar.setProgress((int) progressBarV);
        progressText.setText(progressTextV);
        progressCurrent.setText(progressCurrentV);
        progressLeft.setText(progressLeftV);

        fileValue.setText(fileValueV);
        willEndAtValue.setText(willEndAtValueV);
        totalPrintTimeValue.setText(totalPrintTimeValueV);
        uploadedValue.setText(uploadedValueV);
        filamentValue.setText(filamentValueV);

        tableHotendActual.setText(tableHotendActualV + " °C");
        tableHotendTarget.setText(tableHotendTargetV + " °C");
        tableBedActual.setText(tableBedActualV + " °C");
        tableBedTarget.setText(tableBedTargetV + " °C");

        if (tableHotendActualV < tableHotendTargetV - 5) {
            tableHotendActual.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.border_heating)));
        } else if (tableHotendActualV > tableHotendTargetV + 5) {
            if (tableHotendActualV > 30) {
                tableHotendActual.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.border_cooling)));
            } else {
                tableHotendActual.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.border_default)));
            }
        } else {
            tableHotendActual.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.border_default)));
        }

        if (tableBedActualV < tableBedTargetV - 5) {
            tableBedActual.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.border_heating)));
        } else if (tableBedActualV > tableBedTargetV + 5) {
            if (tableBedActualV > 30) {
                tableBedActual.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.border_cooling)));
            } else {
                tableBedActual.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.border_default)));
            }
        } else {
            tableBedActual.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.border_default)));
        }
        if (connected) {
            control_psu.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.green)));
        } else {
            control_psu.setBackgroundTintList(ColorStateList.valueOf(getResources().getColor(R.color.red)));
        }
    }

    private static double round(double value, int precision) {
        int scale = (int) Math.pow(10, precision);
        return (double) Math.round(value * scale) / scale;
    }

    private String convertSize(int i) {
        if (i < 1024) {
            return i + " B";
        } else if (i < 1048576) {
            return round((double) i / 1024, 2) + " KB";
        } else if (i < 1073741824) {
            return round((double) i / 1048576, 2) + " MB";
        } else {
            return round((double) i / 1073741824, 2) + " GB";
        }
    }

    public void refreshWebcam(ImageView webcam, TextView loading) {
        getWebcamImage();
        getWebcamInfo();
        Thread thread = new Thread(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            if (webcamImage != null) {
                runOnUiThread(() -> {
                    webcam.setImageBitmap(webcamImage);
                    webcamUpdate.setText(webcamUpdateV);
                    loading.setVisibility(View.GONE);
                });
            }
        });
        thread.start();
    }

    private int getWifiState() {
        // 0 = offline
        // 1 = wifi
        // 2 = data
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        if (activeNetwork != null) {
            if (activeNetwork.getType() == ConnectivityManager.TYPE_WIFI) {
                return 1;
            } else if (activeNetwork.getType() == ConnectivityManager.TYPE_MOBILE) {
                return 2;
            }
        }
        return 0;
    }

    private void getNotifications() {
        db.collection("json").document("notifications").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    ArrayList<String> notifications = (ArrayList<String>) document.get("notifications");
                    getWebcamImage();
                    Thread thread = new Thread(() -> {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (webcamImage != null) {
                            if (notifications != null) {
                                //remove duplication
                                Set<String> hs = new HashSet<>(notifications);
                                notifications.clear();
                                notifications.addAll(hs);

                                for (String notification : notifications) {
                                    //100: Print done
                                    //101: Filament change
                                    //102: Filament runout
                                    //200: Refreshed
                                    //201: Refreshed webcam
                                    //202: Cleared logs

                                    //Split at ;
                                    String[] split = notification.split(";");
                                    long time = Long.parseLong(split[1]);

                                    // if time is in usedNotifications, skip
                                    if (usedNotifications.contains(time)) {
                                        continue;
                                    }

                                    usedNotifications.add(time);

                                    switch (split[0]) {
                                        case "100":
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                Notifications.showNotification(MainActivity.this, "Print done!", "Your print is done!", R.drawable.notif_done, webcamImage, time);
                                            }
                                            break;
                                        case "101":
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                Notifications.showNotification(MainActivity.this, "Filament change!", "Please change your filament!", R.drawable.notif_filamentc, webcamImage, time);
                                            }
                                            break;
                                        case "102":
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                                Notifications.showNotification(MainActivity.this, "Filament runout!", "Your filament is running out!", R.drawable.notif_filamentr, webcamImage, time);
                                            }
                                            break;
                                        case "200":
                                            runOnUiThread(() -> ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_refresh_24, "Data refreshed!"));
                                            break;
                                        case "201":
                                            runOnUiThread(() -> ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_photo_camera_24, "Webcam refreshed!"));
                                            break;
                                        case "202":
                                            runOnUiThread(() -> ToastManager.run(MainActivity.this, 3, R.drawable.ic_baseline_playlist_remove_24, "Logs cleared!"));
                                            break;
                                    }
                                }
                                db.collection("json").document("notifications").update("notifications", new ArrayList<>());
                            }
                        }
                    });
                    thread.start();
                }
            }
        });
    }

    private void handleWifiState() {
        if (getWifiState() == 1) {
            // Wifi
            totalCntTime = 3000;
            pausedCntTime = 3000;
            refreshWebcamCntTime = 3000;
            titleWebcam.setText(Html.fromHtml("<b>Webcam</b> (Wifi)"));
            hideControls(Html.fromHtml(""), false);
        } else if (getWifiState() == 2) {
            // Data
            totalCntTime = 5000;
            pausedCntTime = 6000;
            refreshWebcamCntTime = 15000;
            titleWebcam.setText(Html.fromHtml("<b>Webcam</b> (Data)"));
            hideControls(Html.fromHtml(""), false);
        } else {
            totalCntTime = 2500;
            pausedCntTime = 3000;
            refreshWebcamCntTime = 30000;
            hideControls(Html.fromHtml("<b>No internet!</b><br><br><Small>Connect to the internet to use this app.</Small>"), true);
        }
    }

    private void loginToFirebase() {
        hideControls(Html.fromHtml("<b>Loading...</b><br><br><Small>Connecting to FireBase...</Small>"), true);
        FirebaseApp.initializeApp(this);
        if (FirebaseApp.getApps(this).size() == 0) {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(getString(R.string.firestore_email), getString(R.string.firestore_password)).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    hideControls(Html.fromHtml("<b>Loading...</b><br><br><Small>Getting data from FireBase...</Small>"), true);
                    firebaseLogged();
                } else {
                    Log.d("Firebase", "signInWithEmail:failure", task.getException());
                    hideControls(Html.fromHtml("<b>Failed to connect to FireBase!</b><br><br><Small>Check your internet connection and try again.</Small>"), true);
                }
            });
        } else {
            firebaseLogged();
        }
    }

    private void firebaseLogged() {
        if (getWifiState() != 0) {
            getWidgets();
            updateWidgets();
            sharedPreferences();
            jobCommands();
            getNotifications();
            update(true);
            updateLogs();
        } else {
            hideControls(Html.fromHtml("<b>No internet!</b><br><br><Small>Connect to the internet to use this app.</Small>"), true);
        }
        if (getWifiState() == 1) {
            fullWebcam.setVisibility(View.VISIBLE);
            refreshWebcam(webcam, loadingWebcam);
            RelativeLayout.LayoutParams params = (RelativeLayout.LayoutParams) webcamBackground.getLayoutParams();
            params.addRule(RelativeLayout.ALIGN_BOTTOM, R.id.fullWebcam);
            webcamUpdate.setVisibility(View.VISIBLE);
            webcamUpdate.setText(webcamUpdateV);
            webcamBackground.setLayoutParams(params);
            webcamRefresh = true;
        }

        CountDownTimer updateTimerCnt = new CountDownTimer(totalCntTime, 1000) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                if (getWifiState() != 0) {
                    thread.interrupt();
                    getNotifications();
                    update(false);
                    handleWifiState();
                    updateLogs();
                } else {
                    hideControls(Html.fromHtml("<b>No internet!</b><br><br><Small>Connect to the internet to use this app.</Small>"), true);
                }
                toggleService();
                start();
            }
        };
        updateTimerCnt.start();

        CountDownTimer refreshWebcamCnt = new CountDownTimer(refreshWebcamCntTime, 1000) {
            @Override
            public void onTick(long l) {
            }

            @Override
            public void onFinish() {
                if (getWifiState() != 0) {
                    if (webcamRefresh && getWifiState() != 0) {
                        refreshWebcam(webcam, loadingWebcam);
                    }
                } else {
                    hideControls(Html.fromHtml("<b>No internet!</b><br><br><Small>Connect to the internet to use this app.</Small>"), true);
                }
                start();
            }
        };
        refreshWebcamCnt.start();

        CountDownTimer pausedTimerCnt = new CountDownTimer(pausedCntTime, 1000) {
            @Override
            public void onTick(long l) {

            }

            @Override
            public void onFinish() {
                db.collection("json").document("pauseStatus").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        paused = Boolean.TRUE.equals(task.getResult().getBoolean("paused"));
                    }
                });
                start();
            }
        };
        pausedTimerCnt.start();
    }

    private void hideControls(Spanned message, boolean hide) {
        root = findViewById(R.id.root_layout);
        lockControls = findViewById(R.id.lockControls);
        loading_root = findViewById(R.id.loading_root);
        if (hide) {
            root.setVisibility(View.INVISIBLE);
            lockControls.setVisibility(View.INVISIBLE);
            loading_root.setVisibility(View.VISIBLE);
            loading_root.setText(message);
        } else {
            root.setVisibility(View.VISIBLE);
            lockControls.setVisibility(View.VISIBLE);
            loading_root.setVisibility(View.INVISIBLE);
        }
    }

    private void toggleService() {
        if (getWifiState() == 2) {
            // Data - stop service
            if (isMyServiceRunning(KeepAlive.class)) {
                stopService(new Intent(this, KeepAlive.class));
            }
        } else if (getWifiState() == 1 && connected) {
            // Wifi - start service
            if (!isMyServiceRunning(KeepAlive.class)) {
                startService(new Intent(this, KeepAlive.class));
            }
        }
    }

    public boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void getWebcamImage() {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference islandRef = storageRef.child("webcam.jpg");
        final long ONE_MEGABYTE = 1024 * 1024;
        islandRef.getBytes(ONE_MEGABYTE).addOnSuccessListener(bytes -> {
            webcamImage = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        });
    }

    public void getWebcamInfo() {
        StorageReference storageRef = FirebaseStorage.getInstance().getReference();
        StorageReference islandRef = storageRef.child("webcam.jpg");
        islandRef.getMetadata().addOnSuccessListener(storageMetadata -> {
            Date webcamDate = getDate(storageMetadata.getUpdatedTimeMillis());
            String webcamDateStr = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.ENGLISH).format(webcamDate);
            webcamUpdateV = "From " + webcamDateStr;
        });
    }

    public Date getDate(long mills) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(mills);
        return calendar.getTime();
    }

    private void updateLogs() {
        db.collection("json").document("log").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                DocumentSnapshot document = task.getResult();
                if (document.exists()) {
                    try {
                        JSONObject data = new JSONObject(Objects.requireNonNull(Objects.requireNonNull(task.getResult()).getData()));
                        JSONArray logs = data.getJSONArray("history");
                        StringBuilder logText = new StringBuilder();
                        for (int i = logs.length() - 1; i >= logs.length() - 10; i--) {
                            JSONObject log = logs.getJSONObject(i);
                            // [time] [type] [service] - [message]
                            String logStr = "<font color=" + getColor("cyan") + ">[" + log.getString("time") + "]</font> <font color=" + getColor(log.getString("typeColor")) + ">[" + log.getString("type") + "]</font> <font color=" + getColor(log.getString("serviceColor")) + ">[" + log.getString("service") + "]</font> - " + log.getString("message");
                            logText.append(logStr);
                            if (i != logs.length() - 10) {
                                logText.append("<br><br>");
                            }
                        }
                        if (logsHidden) {
                            showLogs.setText("Logs are hidden.");
                        } else {
                            showLogs.setText(Html.fromHtml(logText.toString()));
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public String getColor(String color) {
        String colorHex = "#FFFFFF";
        switch (color) {
            case "magenta":
                colorHex = "#BB00FF";
                break;
            case "yellow":
                colorHex = "#FF9500";
                break;
            case "green":
                colorHex = "#36A303";
                break;
            case "red":
                colorHex = "#B51504";
                break;
            case "cyan":
                colorHex = "#04B0C7";
                break;
        }
        return colorHex;
    }
}
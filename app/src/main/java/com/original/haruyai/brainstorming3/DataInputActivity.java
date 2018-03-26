package com.original.haruyai.brainstorming3;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;


/**
 * Created by Haruya on 2017/11/08.
 */

public class DataInputActivity extends ActionBarActivity {
    NumberPicker numPicker;
    Button button1;
    Button button2;

    //リスト画面からIDをもらうための変数
    private String newID;
    //テーマを格納するところ
    private String talktheme = "";
    //参加者を格納するところ
    private String participants = "";
    //日付を格納するところ
    private String date = "";

    //会議ごとのIDを配列で．Gsonの処理がややこしいからString型にして，intentを通すためにListじゃなくてArrayListで宣言してる
    private ArrayList<String> meetingID = new ArrayList<>();

    //Actionbarのアイコンを非表示にするため
    MenuItem item;

    //ここでactionbar_menu.xmlをActionBar上に表示している
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.actionbar_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }
    //バブルアイコンを非表示にしておくための関数
    @Override
    public boolean onPrepareOptionsMenu(Menu menu){
        super.onPrepareOptionsMenu(menu);
        //バブルアイコンを非表示に
        item = menu.findItem(R.id.bubble_icon);
        item.setVisible(false);
        return true;
    }

    //ActionBar上のアイコン（Item）が押されたことを検知するための関数
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        //IDごとにボタンを押した際の処理を記述
        switch (item.getItemId()) {
            //戻るアイコンが押された時
            case android.R.id.home:
                finish();
                break;
        }
        return true;
    }

    @SuppressLint("WrongViewCast")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datainput);

        // ActionBarの設定
        if (savedInstanceState == null) {
            // customActionBarの取得
            View customActionBarView = this.getActionBarView("Hoge", "表示する画像のURL");
            // ActionBarの取得
            android.support.v7.app.ActionBar actionBar = this.getSupportActionBar();
            // 戻るボタンを表示するかどうか('<' <- こんなやつ)
            actionBar.setDisplayHomeAsUpEnabled(true);
            // タイトルを表示するか（もちろん表示しない）
            actionBar.setDisplayShowTitleEnabled(false);
            // iconを表示するか（もちろん表示しない）
            actionBar.setDisplayShowHomeEnabled(false);
            // ActionBarにcustomViewを設定する
            actionBar.setCustomView(customActionBarView);
            // CustomViewを表示するか
            actionBar.setDisplayShowCustomEnabled(true);
        }

        //テーマの入力部分の定義
        final EditText talktheme_area = (EditText) findViewById(R.id.editText);

        // 遷移元からIDを受け取る
        Intent intent = getIntent();
        newID = intent.getStringExtra("newID");
        meetingID = intent.getStringArrayListExtra("meetingID");

        findViews();
        initViews();

        button2 = (Button)findViewById(R.id.button2);
        button2.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                //トークテーマをarrayに追加
                talktheme = talktheme_area.getText().toString();
                //参加者をarrayに追加
                for(int i=1; i <= numPicker.getValue(); i++){
                    EditText participants_area = (EditText) findViewById(i);
                    //最後尾じゃない時
                    if(i!=numPicker.getValue()){
                        participants += participants_area.getText().toString() + ", ";
                    }
                    //最後尾の時
                    else{
                        participants += participants_area.getText().toString();
                    }
                }

                //日付取得--------------------------------------------------------------------------
                Calendar cal = Calendar.getInstance(); //import必要
                //めんどくさいことに曜日が定数で返ってくる
                int dow = cal.get(Calendar.DAY_OF_WEEK);
                String youbi = "";

                switch (dow){
                    case 1:
                        youbi = "(日)";
                        break;
                    case 2:
                        youbi = "(月)";
                        break;
                    case 3:
                        youbi = "(火)";
                        break;
                    case 4:
                        youbi = "(水)";
                        break;
                    case 5:
                        youbi = "(木)";
                        break;
                    case 6:
                        youbi = "(金)";
                        break;
                    case 7:
                        youbi = "(土)";
                        break;
                }
                //なんでか知らんけど１月から定数で０で返るようになっているから+1している
                int month = cal.get(Calendar.MONTH) + 1;

                //「月/日(曜日)」で表示するように
                date = "\n" + month + "/" + cal.get(Calendar.DATE) + youbi;

                // 遷移先のactivityを指定intentを作成
                Intent intent1 = new Intent(getApplication(), SpeechRecognition.class);
                // 遷移先へID，テーマ，参加者，日付のそれぞれの変数を渡す
                //intent1.putExtra("キー", array);
                intent1.putExtra("newID", newID);
                intent1.putExtra("theme", talktheme);
                intent1.putExtra("participants", participants);
                intent1.putExtra("date", date);
                intent1.putStringArrayListExtra("meetingID", meetingID);
                // 指定のActivityを開始する
                startActivity(intent1);

            }
        });
    }

    private void findViews(){
        numPicker = (NumberPicker)this.findViewById(R.id.numPicker);
        button1 = (Button)findViewById(R.id.button1);
    }

    private void initViews(){
        numPicker.setMaxValue(20);
        numPicker.setMinValue(1);

        button1.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v) {
                setView();
            }
        });
    }

    private void setView(){
        LinearLayout layout = (LinearLayout) findViewById(R.id.part_view);
        layout.removeAllViews();
        for (int i = 1; i <= numPicker.getValue(); ++i) {
            View view = getLayoutInflater().inflate(R.layout.content_data, null);
            layout.addView(view);
            EditText text = (EditText) view.findViewById(R.id.text);
            text.setId(i);
            text.setHint("参加者" + i);
        }
    }

    //ActionBarを設定するための関数
    public View getActionBarView(String title, String imageURL) {

        // 表示するlayoutファイルの取得
        LayoutInflater inflater = LayoutInflater.from(this);
        final View view = inflater.inflate(R.layout.custom_action_bar, null);

        // CustomViewにクリックイベントを登録する
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

            }
        });

        return view;

    }

}
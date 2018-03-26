package com.original.haruyai.brainstorming3;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

//主な機能は，リストの表示と，新規作成時の値の受け渡し
public class ListActivity extends ActionBarActivity {

    //会議ごとのIDを配列で．Gsonの処理がややこしいからString型にして，intentを通すためにListじゃなくてArrayListで宣言してる
    private ArrayList<String> meetingID = new ArrayList<>();

    //meetingIDの長さを覚えておくための変数
    int meetingID_length = 0;
    //meetingIDが新しく追加された時の新しいIDを遷移先に渡す用
    String newID;
    //[テーマ，参加者，日付，要約，アイデアマップの有無]
    private List<String> meetingDataArray = new ArrayList<>();

    //Actionbarのアイコンを非表示にするため
    MenuItem item;

    //閲覧用かどうかをSummary, MainActivity, Mappingに知らせるための変数. trueのとき閲覧用
    private boolean viewMode = false;
    //リストが押された時に閲覧用の議事録画面に渡すための変数
    private String minutes;
    private String summary;
    private String keywords;
    private String participants;
    private String theme;
    private String date;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

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

        //会議ごとのIDをストレージから読み取り(meetingIDだけsharedpreferencesを独自の"meeting"にしてる)--------------------------------------------------------------------------
        Gson gson = new Gson();
        SharedPreferences sharedPreferences = getSharedPreferences("meeting", Context.MODE_PRIVATE);
        //空かどうかチェックするために一回getStringのところで段階踏む
        String mID = sharedPreferences.getString("meetingID", null);
        //会議IDが存在しないとき→リストを表示する必要がない
        if(mID == null){
            //会議IDを新しく生成
            meetingID.add("1");
            //会議IDの長さを取得->1になってくれるはず
            meetingID_length = meetingID.size();
            //会議IDがすでにある時とちょっと動かしかたが違う
            newID = "1";
            //会議IDがない時はDataInputActivityにすぐ遷移する
            Intent intent_toSR = new Intent(getApplication(), DataInputActivity.class);
            //変数(全ノードのリスト)を渡すとこ
            //とりあえず新しく生成したmeetingIDを渡す
            intent_toSR.putExtra("newID", newID);
            intent_toSR.putStringArrayListExtra("meetingID", meetingID);
            startActivity(intent_toSR);
        }
        //会議IDが存在する時
        else{
            //リストに表示するところの色々を定義
            ArrayList<HashMap<String, String>> list_data = new ArrayList<HashMap<String, String>>();
            HashMap<String, String> hashTmp = new HashMap<String, String>();
            ListView listView = (ListView) findViewById(R.id.listView1);

            //ストレージから会議IDを文字列の配列として取得
            meetingID = gson.fromJson(mID, new TypeToken<List>(){}.getType());
            //会議IDの長さを取得
            meetingID_length =  meetingID.size();
            //newIDを定義する．文字列->数字->数字+1->文字列
            newID = meetingID.get(meetingID_length-1);
            //削除をちゃんと実装すれば上書きされて問題ないはず
            int n = Integer.parseInt(meetingID.get(meetingID_length-1));
            n += 1;
            newID = String.valueOf(n);
            
            //会議IDの数だけ回す
            for(int m=0; m<meetingID_length; m++){
                //会議ごとのデータをストレージから読み取り
                Gson gson1 = new Gson();
                SharedPreferences sharedPreferences1 = getSharedPreferences("shared" + meetingID.get(m), Context.MODE_PRIVATE);
                //リスト表示用の配列をロードする．会議終了ボタンで保存されたやつ. [テーマ，参加者，日付，要約，アイデアマップの有無]
                meetingDataArray = gson.fromJson(sharedPreferences1.getString("meetingDataArray" + meetingID.get(m), null), new TypeToken<List>(){}.getType());

                //HashMapに入れていって，それをリストに格納していく
                hashTmp.clear();
                hashTmp.put("theme", meetingDataArray.get(0));
                hashTmp.put("participants", meetingDataArray.get(1));
                hashTmp.put("date", meetingDataArray.get(2));
                hashTmp.put("summary", meetingDataArray.get(3));
                list_data.add(new HashMap<>(hashTmp));

            }

            //HashMapのキーから値を一括で取ってきて表示できる
            SimpleAdapter simp = new SimpleAdapter(getApplicationContext(), list_data, R.layout.two_line_list_item,
                    new String[]{"theme", "participants", "date", "summary"}, new int[]{R.id.item_theme, R.id.item_created_info, R.id.date_area, R.id.item_highlight});

            ((ListView)findViewById(R.id.listView1)).setAdapter(simp);

            //その会議でアイデアマップが作成されていない時
            //if(meetingDataArray.get(4).equals("0")){
                //ImageView bubble_icon = (ImageView)findViewById(R.id.bubble_icon);
                //bubble_icon.setColorFilter(Color.parseColor("#aaaaaa"), PorterDuff.Mode.SRC_IN);
            //}

            //ImageView trash_button = (ImageView) findViewById(R.id.item_trashicon);
            //trash_button.setColorFilter(Color.parseColor("#aaaaaa"));

            //リストがクリックされた時．それぞれのリストのインデントがわかる
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view,
                                        int position, long id) {
                    ListView listView_p = (ListView) parent;
                    // クリックされたリストのインデックスを取得（0から
                    String list_index_string = String.valueOf(position);
                    int list_index = Integer.parseInt(list_index_string);

                    //対応する会議IDを取得
                    String clickedID = meetingID.get(list_index);

                    //押されたリストに対応するIDでロードする
                    SharedPreferences sharedPreferences_listclicked = getSharedPreferences("shared" + clickedID, Context.MODE_PRIVATE);
                    //会議ID
                    minutes = sharedPreferences_listclicked.getString("minutes" + clickedID, "");
                    summary= sharedPreferences_listclicked.getString("summary" + clickedID, "");
                    keywords = sharedPreferences_listclicked.getString("keywords" + clickedID, "");
                    participants = sharedPreferences_listclicked.getString("participants" + clickedID, "");
                    theme = sharedPreferences_listclicked.getString("theme" + clickedID, "");
                    date = sharedPreferences_listclicked.getString("date" + clickedID, "");

                    //閲覧モードをONにして渡す
                    viewMode = true;

                    //遷移
                    Intent intent_4View = new Intent(getApplication(), Summary.class);
                    //変数(全ノードのリスト)を渡すとこ
                    //とりあえず新しく生成したmeetingIDを渡す
                    intent_4View.putExtra("clickedID", clickedID);
                    intent_4View.putExtra("minutes", minutes);
                    intent_4View.putExtra("summary", summary);
                    intent_4View.putExtra("keywords", keywords);
                    intent_4View.putExtra("viewMode", viewMode);
                    intent_4View.putExtra("participants", participants);
                    intent_4View.putExtra("theme", theme);
                    intent_4View.putExtra("date", date);
                    startActivity(intent_4View);
                }
            });
        }


        //右下プラスボタンが押された時用
        View plusmark = findViewById(R.id.add);
        plusmark.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //会議ごとのIDをストレージに一旦保存する．(meetingIDだけsharedpreferencesを独自の"meeting"にしてる)--------------------------------------------------------------------------
                //会議終了時にこれを読み取ってnewIDを追加したのちに保存して更新する
                //Gson gson2 = new Gson();
                //SharedPreferences sharedPreferences2 = getSharedPreferences("meeting", Context.MODE_PRIVATE);
                //SharedPreferences.Editor editor = sharedPreferences2.edit();
                //会議ごとのIDを配列として新しく保存する
                //editor.putString("meetingID", gson2.toJson(meetingID));
                //editor.apply();


                //遷移
                Intent intent_toSR = new Intent(getApplication(), DataInputActivity.class);
                //変数(全ノードのリスト)を渡すとこ
                //とりあえず新しく生成したmeetingIDを渡す
                intent_toSR.putExtra("newID", newID);
                intent_toSR.putStringArrayListExtra("meetingID", meetingID);
                startActivity(intent_toSR);

                /*
                // テキスト入力用Viewの作成
                final EditText editView = new EditText(ListActivity.this);
                AlertDialog.Builder dialog = new AlertDialog.Builder(ListActivity.this);
                dialog.setTitle("トークテーマ");
                dialog.setView(editView); // OKボタンの設定
                dialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // OKボタンをタップした時の処理をここに記述
                        // トークテーマを取得　あとはサーバーに送ってください
                        talktheme = editView.getText().toString();
                        //インテントの作成
                        Intent intent = new Intent(getApplication(), android.app.ListActivity.class); //本来はこれでいく
                        //遷移先の画面を起動
                        // StartActivity(intent);
                    }
                });

                // キャンセルボタンの設定
                dialog.setNegativeButton("キャンセル", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // キャンセルボタンをタップした時の処理をここに記述
                        dialog.cancel();
                    }
                });
                dialog.show();
            }
            */

            }
        });
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
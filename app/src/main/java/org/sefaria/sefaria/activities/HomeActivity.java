package org.sefaria.sefaria.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import org.sefaria.sefaria.BuildConfig;
import org.sefaria.sefaria.GoogleTracker;
import org.sefaria.sefaria.MenuElements.MenuDirectRef;
import org.sefaria.sefaria.Settings;
import org.sefaria.sefaria.database.Book;
import org.sefaria.sefaria.database.DailyLearning;
import org.sefaria.sefaria.database.Database;
import org.sefaria.sefaria.database.Huffman;
import org.sefaria.sefaria.database.Searching;
import org.sefaria.sefaria.layouts.CustomActionbar;
import org.sefaria.sefaria.R;
import org.sefaria.sefaria.Util;
import org.sefaria.sefaria.MenuElements.MenuGrid;
import org.sefaria.sefaria.MenuElements.MenuNode;
import org.sefaria.sefaria.MenuElements.MenuState;
import org.sefaria.sefaria.layouts.SefariaTextView;

import android.util.Log;
import android.util.Pair;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;


public class HomeActivity extends Activity {

    private final int NUM_COLUMNS = 3;
    private final boolean LIMIT_GRID_SIZE = true;

    private MenuGrid menuGrid;
    private MenuState menuState;
    private boolean isPopup;
    private List<MenuDirectRef> dailyLearnings;
    private List<MenuDirectRef> recentTexts;
    private LinearLayout recentRoot;

    @Override
    protected void onCreate(Bundle in) {
        super.onCreate(in);
        setTheme(Settings.getTheme());
        setContentView(R.layout.fragment_home);
        Huffman.makeTree(true);
        Log.d("HomeAct", "running onCreate");

        Intent intent = getIntent();

        menuState = intent.getParcelableExtra("menuState");
        isPopup = intent.getBooleanExtra("isPopup",false);

        if (in != null) {
            menuState = in.getParcelable("menuState");
        }

        if (menuState == null) {
            menuState = new MenuState();
        }

        LinearLayout gridRoot = (LinearLayout) findViewById(R.id.gridRoot);
        LinearLayout homeRoot = new LinearLayout(this);
        homeRoot.setOrientation(LinearLayout.VERTICAL);
        homeRoot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        homeRoot.setGravity(Gravity.CENTER);
        gridRoot.addView(homeRoot);

        if (!isPopup) { //Don't mention the living library thing unless you're actually at the home screen
            addHeader(homeRoot);
        }
        addMenuGrid(homeRoot);
        addRecentTexts(homeRoot);
        addCalendar(homeRoot);


        //just extra spacing for the bottom
        homeRoot.addView(createTypeTitle("",false));

        LayoutInflater inflater = (LayoutInflater) getSystemService(Activity.LAYOUT_INFLATER_SERVICE);


        //toggle closeClick, depending on if menu is popup or not
        View.OnClickListener tempCloseClick = null;
        if (isPopup) tempCloseClick = closeClick; //Removing the close click for now to test without it


        LinearLayout abRoot = (LinearLayout) findViewById(R.id.actionbarRoot);
        CustomActionbar cab = new CustomActionbar(this,new MenuNode("Sefaria","ספאריה",null),
                Settings.getSystemLang(),null,null,tempCloseClick,searchClick,null,menuClick,null,-1);
        abRoot.addView(cab);

        if(intent.getBooleanExtra("hideOpening",false)){
            onBackPressed();
        }

    }

    private boolean veryFirstTime = true;

    @Override
    protected void onResume() {
        super.onResume();
        if(!veryFirstTime) {
            Huffman.makeTree(true);
            addRecentTexts(null);
            setLang(Settings.getMenuLang());
        }else
            veryFirstTime = false;
        GoogleTracker.sendScreen("HomeActivity");

    }

    private void addHeader(LinearLayout homeRoot){
        //Living Library
        TextView livingLibraryView = createTypeTitle("A Living Library of Jewish Texts",true);
        livingLibraryView.setTextSize(20);
        int livingPadding = 60;
        livingLibraryView.setPadding(3, livingPadding, 3, livingPadding);
        livingLibraryView.setTextColor(Util.getColor(this, R.attr.text_color_main));
        homeRoot.addView(livingLibraryView);
    }

    private void addRecentTexts(LinearLayout homeRoot){
        //Recent Texts
        if(recentRoot == null) {
            recentRoot = new LinearLayout(this);
            recentRoot.setOrientation(LinearLayout.VERTICAL);
            recentRoot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
            homeRoot.addView(recentRoot);
        }
        else{
            recentRoot.removeAllViews();
        }
        final int columNum = 3;
        List<String> recentBooks = Settings.RecentTexts.getRecentTexts();
        recentTexts = new ArrayList<>();
        if(recentBooks.size()>0) {
            recentRoot.addView(createTypeTitle("Recent Texts",false));
            LinearLayout recentRow = null;
            for (int i=0;i<recentBooks.size();i++){
                if(i%columNum  == 0){
                    recentRow = new LinearLayout(this);
                    recentRow.setOrientation(LinearLayout.HORIZONTAL);
                    recentRow.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
                    recentRoot.addView(recentRow);
                }

                String bookTitle = recentBooks.get(i);
                Book book = null;
                try {
                    book = new Book(bookTitle);
                    Pair<String,String> pair = Settings.BookSettings.getSavedBookTitle(bookTitle);
                    MenuDirectRef menuDirectRef = new MenuDirectRef(this, pair.first, pair.second, null, book, null);
                    menuDirectRef.setLongClickPinning();
                    recentTexts.add(menuDirectRef);
                    recentRow.addView(menuDirectRef);
                } catch (Exception e) {
                    Log.e("HomeActivity", "Problem getting Recent Texts:" + e.getMessage());
                }

            }
        }
        /*
                    //add 'more' button in the row which was overflowed
            if (Math.floor(HOME_MENU_OVERFLOW_NUM/numColumns) == i+1 && limitGridSize)
                addMoreButton(ll);
         */

    }




    private void addMenuGrid(LinearLayout homeRoot){
        //Menu grid
        Util.Lang menuLang = Settings.getMenuLang();
        menuGrid = new MenuGrid(this,NUM_COLUMNS, menuState,LIMIT_GRID_SIZE,menuLang);
        homeRoot.addView(createTypeTitle("Browse Texts",false));
        homeRoot.addView(menuGrid);
    }

    private void setLang(Util.Lang lang){
        if(lang == Util.Lang.BI) {
            lang = Util.Lang.EN;
        }
        menuState.setLang(lang);
        menuGrid.setLang(lang);
        //not setting cab, b/c it should stay as the SystemLang
        for(MenuDirectRef menuDirectRef:dailyLearnings)
            menuDirectRef.setLang(lang);

        for(MenuDirectRef menuDirectRef:recentTexts){
            menuDirectRef.setLang(lang);
        }

    }

    private void addCalendar(LinearLayout homeRoot){
        //Calendar
        LinearLayout calendarRoot = new LinearLayout(this);
        calendarRoot.setOrientation(LinearLayout.HORIZONTAL);
        calendarRoot.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        homeRoot.addView(createTypeTitle("Calendar", false));
        homeRoot.addView(calendarRoot);
        dailyLearnings = DailyLearning.getDailyLearnings(this);
        for(MenuDirectRef menuDirectRef: dailyLearnings) {
            calendarRoot.addView(menuDirectRef);
        }
    }

    private TextView createTypeTitle(String title, boolean isSerif){
        SefariaTextView textView = new SefariaTextView(this);
        textView.setText(title);
        final int paddingSide= 3;
        final int paddingTop = 20;
        textView.setPadding(paddingSide, paddingTop * 2, paddingSide, paddingTop);
        textView.setTextSize(20);
        textView.setFont(Util.Lang.EN, isSerif); //TODO change with system lang
        textView.setTextColor(Util.getColor(this, R.attr.text_color_english));
        textView.setGravity(Gravity.CENTER);
        if (! isSerif && Build.VERSION.SDK_INT > 14) {
            textView.setAllCaps(true);
        }

        return textView;
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        menuGrid.setLang(menuGrid.getLang());
    }

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);

        out.putParcelable("menuState", menuState);
    }



    View.OnClickListener searchClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            Intent intent = new Intent(HomeActivity.this,SearchActivity.class);
            startActivity(intent);
            /*final SchemeRegistry schemeRegistry = new SchemeRegistry();
            schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            schemeRegistry.register(new Scheme("https", MyApp.createSefariaSSLSocketFactory(), 443));

            // and then however you create your connection manager, I use ThreadSafeClientConnManager
            final HttpParams params = new BasicHttpParams();
            final PoolingClientConnManager cm = new PoolingClientConnManager(params,schemeRegistry);

            String connectionUrl = API.SEARCH_URL;
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            ElasticClient client;
            try {
                ConnectorSettings settings
                        = ConnectorSettings
                        .builder()
                        .baseUrl(API.SEARCH_URL)
                        .build();

                client = new ElasticClient(settings);
                List<String> yo = client.search("{\"query\":{\"match_all\": {}}}", String.class);
                Log.d("HomeActivity", ""+yo.size());
            } catch (URISyntaxException e) {
                e.printStackTrace();
                Log.e("HomeActivity", e.getMessage());
            }*/
            /*JestClientFactory factory = new JestClientFactory();
            factory.setDroidClientConfig(new DroidClientConfig
                    .Builder(connectionUrl)
                    .multiThreaded(true)
                    .build());
            JestClient client = factory.getObject();

            SearchSour*/
        }
    };



    View.OnClickListener closeClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            finish();
            overridePendingTransition(R.animator.stay, R.animator.slide_left);
        }
    };

    View.OnClickListener menuClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            setLang(Settings.switchMenuLang());
        }
    };

    public void settingsClick(View v) {
        Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    public void aboutClick(View v) {
        String url = "https://sefaria.org/about";
        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
        startActivity(intent);
    }

    private static Searching searching;
    public void feedbackClick(View v) {

        /*
        try {
            if(searching == null) {
                boolean alsoSearchCommentary = true;
                searching = new Searching("ברא", new LinkFilter("Tosefta", 0, "", LinkFilter.DEPTH_TYPE.CAT),alsoSearchCommentary);
                //searching = new Searching("ברא", null,alsoSearchCommentary);
            }
            ArrayList<Text> results = searching.getResults();
            results = API.getSearchResults("love",null,0,10);

            Log.d("Searching", "results.size" + results.size());
            for(Text verse:results){
                verse.log();
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        if(true) return;
        */

        String email = "android@sefaria.org";
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                "mailto", email, null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Android App Feedback");
        emailIntent.putExtra(Intent.EXTRA_TEXT, getEmailHeader());
        emailIntent.putExtra(Intent.EXTRA_EMAIL, new String [] {email});
        startActivity(Intent.createChooser(emailIntent, "Send email"));
    }

    public static String getEmailHeader(){
        return  "App Version: " + BuildConfig.VERSION_NAME + " ("  + BuildConfig.VERSION_CODE + ")" + "\n"
                + "Online Library Version: " + Util.convertDBnum(Database.getVersionInDB(true)) + "\n"
                + "Offline Library Version: " + Util.convertDBnum(Database.getVersionInDB(false)) + "\n"
                + "Using " + (Settings.getUseAPI()? "Online":"Offline") + " Library" + "\n"
                + GoogleTracker.randomID + "\n"
                + Build.VERSION.RELEASE + " (" + Build.VERSION.SDK_INT + ")" + "\n"
                +"\n\n\n";
    }

    public void siteClick(View v){
        String url = "https://sefaria.org";
        Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse(url));
        startActivity(intent);
    }

}

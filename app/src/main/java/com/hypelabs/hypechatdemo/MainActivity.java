//
// MIT License
//
// Copyright (C) 2018 HypeLabs Inc.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//

package com.hypelabs.hypechatdemo;

import android.app.Activity;
import android.graphics.Color;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import com.hypelabs.hype.Hype;
import com.hypelabs.hype.Instance;
import com.hypelabs.hype.Message;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import devlight.io.library.ntb.NavigationTabBar;

public class MainActivity extends Activity implements Store.Delegate {

    public static String INTENT_EXTRA_STORE = "com.hypelabs.store";
    private ChatViewAdapter adapter;

    protected Message sendMessage(String text, Instance instance) throws UnsupportedEncodingException {

        // When sending content there must be some sort of protocol that both parties
        // understand. In this case, we simply send the text encoded in UTF-8. The data
        // must be decoded when received, using the same encoding.
        byte[] data = text.getBytes("UTF-8");

        // Sends the data and returns the message that has been generated for it. Messages have
        // identifiers that are useful for keeping track of the message's deliverability state.
        // In order to track message delivery set the last parameter to true. Notice that this
        // is not recommend, as it incurs extra overhead on the network. Use this feature only
        // if progress tracking is really necessary.
        return Hype.send(data, instance, false);
    }

    @Override
    protected void onStart() {

        super.onStart();

        setContentView(R.layout.activity_main);
        initLayout();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

    }

    private void initLayout() {
        final ViewPager viewPager = findViewById(R.id.vp_horizontal_ntb);
        viewPager.setAdapter(new PagerAdapter() {
            @Override
            public int getCount() {
                return 4;
            }

            @Override
            public boolean isViewFromObject(final View view, final Object object) {
                return view.equals(object);
            }

            @Override
            public void destroyItem(final View container, final int position, final Object object) {
                ((ViewPager) container).removeView((View) object);
            }

            @Override
            public Object instantiateItem(final ViewGroup container, final int position) {

                View view = new View(getApplicationContext());

                switch (position){
                    case 0:
                        view = initAlertLayout();
                        break;
                }

                container.addView(view);
                return view;
            }
        });

        final NavigationTabBar tabBar = (NavigationTabBar) findViewById(R.id.ntb_horizontal);
        final ArrayList<NavigationTabBar.Model> model = new ArrayList<>();
        model.add(
                new NavigationTabBar.Model.Builder(
                        getResources().getDrawable(R.drawable.ic_fifth), Color.parseColor("#686de0")
                ).build()
        );
        model.add(
                new NavigationTabBar.Model.Builder(
                        getResources().getDrawable(R.drawable.ic_first), Color.parseColor("#686de0")
                ).build()
        );
        model.add(
                new NavigationTabBar.Model.Builder(
                        getResources().getDrawable(R.drawable.ic_fourth), Color.parseColor("#686de0")
                ).build()
        );
        model.add(
                new NavigationTabBar.Model.Builder(
                        getResources().getDrawable(R.drawable.ic_sixth), Color.parseColor("#686de0")
                ).build()
        );
        tabBar.setModels(model);
        tabBar.setViewPager(viewPager, 0);
        tabBar.setOnTabBarSelectedIndexListener(new NavigationTabBar.OnTabBarSelectedIndexListener() {
            @Override
            public void onStartTabSelected(final NavigationTabBar.Model model, final int index) {

            }

            @Override
            public void onEndTabSelected(final NavigationTabBar.Model model, final int index) {
                // Toast.makeText(getApplicationContext(), String.format("onEndTabSelected #%d", index), Toast.LENGTH_SHORT).show();
            }
        });
    }


    public View initAlertLayout(){
        View view = LayoutInflater.from(getBaseContext()).inflate(R.layout.chat_view, null, false);
        final ListView listView = (ListView) view.findViewById(R.id.list_view);
        final Button sendButton = (Button) view.findViewById(R.id.send_button);
        final EditText messageField = (EditText) view.findViewById(R.id.message_field);

         final Store store = getStore();

         adapter = new ChatViewAdapter(this, store);
         listView.setAdapter(adapter);

        TextView announcement = view.findViewById(R.id.chatViewInstanceAnnouncement);
            try {
                 announcement.setText(new String(store.getInstance().getAnnouncement(), "UTF-8"));
          } catch (UnsupportedEncodingException e) {
        e.printStackTrace();
           }

        // Start listening to message queue events so we can update the UI accordingly
          store.setDelegate(this);

        // Flag all messages as read
         store.setLastReadIndex(store.getMessages().size());

        sendButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {

                String text = messageField.getText().toString();

                // Avoids accidental presses
                if (text == null || text.length() == 0) {
                    return;
                }

                try {
                Log.v(getClass().getSimpleName(), "send message");
                    Message message = sendMessage(text, store.getInstance());

                 if (message != null) {

                // Clear message content
                  messageField.setText("");

                // Add the message to the store so it shows in the UI
                     store.add(message);
                  }

                  } catch (UnsupportedEncodingException e) {
                      e.printStackTrace();
                 }
            }
        });

        return view;
    }

    @Override
    public void onBackPressed() {

        super.onBackPressed();

        if(getStore() != null){
            getStore().setLastReadIndex(getStore().getMessages().size());
        }
    }

    @Override
    public void onMessageAdded(Store store, Message message) {

        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                View view = LayoutInflater.from(getBaseContext()).inflate(R.layout.chat_view, null, false);
                final ListView listView = (ListView) view.findViewById(R.id.list_view);

                adapter.notifyDataSetChanged();
            }
        });
    }

    protected Store getStore() {

        ChatApplication chatApplication = (ChatApplication)getApplication();
        String storeIdentifier = getIntent().getStringExtra(INTENT_EXTRA_STORE);
        Store store = chatApplication.getStores().get(storeIdentifier);

        return store;
    }
}
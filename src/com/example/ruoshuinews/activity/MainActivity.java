package com.example.ruoshuinews.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.example.ruoshuinews.R;
import com.example.ruoshuinews.custom.CustomSimpleAdapter;
import com.example.ruoshuinews.util.DensityUtil;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.GridView;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class MainActivity extends Activity {

	
	private final int COMUMNWIDTHPX = 100;	// 新闻分类的宽度
	private final int FLINGVELOCITYPX =800;	// 滚动距离
	
	private int mColumnWidthDip;
	private int mFlingVelocityDip;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_main);

//		if (savedInstanceState == null) {
//			getSupportFragmentManager().beginTransaction()
//					.add(R.id.container, new PlaceholderFragment()).commit();
//		}
		
		//将px转换为dp
		mColumnWidthDip = DensityUtil.px2dip(this, COMUMNWIDTHPX);
		mFlingVelocityDip = DensityUtil.px2dip(this, FLINGVELOCITYPX);
		
		//新闻分类
		String[] categoryArray = getResources().getStringArray(R.array.categories);
		//把新闻分类保存到List中
		List<HashMap<String, String>> categories = new ArrayList<HashMap<String, String>>();
		for (int i = 0; i < categoryArray.length; i++) {
			HashMap<String, String> hashMap = new HashMap<String, String>();
			hashMap.put("category_title", categoryArray[i]);
			categories.add(hashMap);
		}
		
		//创建Adapter，指明映射字段
		CustomSimpleAdapter categoryAdapter = new CustomSimpleAdapter(this, categories, R.layout.category_title, new String[]{"category_title"}, new int[]{R.id.category_title});
		
		//新建GridView
		GridView category = new GridView(this);
		category.setColumnWidth(mColumnWidthDip);
		category.setNumColumns(GridView.AUTO_FIT);
		category.setGravity(Gravity.CENTER);
		//根据单元格宽度和数目计算总宽度
		int width = mColumnWidthDip * categories.size();
		LayoutParams params = new LayoutParams(width, LayoutParams.WRAP_CONTENT);
		category.setLayoutParams(params);
		category.setAdapter(categoryAdapter);
		
		category.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> arg0, View arg1, int arg2,
					long arg3) {
				TextView categoryTitle;
				//恢复每个单元格背景色
				for (int i = 0; i < arg0.getChildCount(); i++)
				{
					categoryTitle = (TextView) (arg0.getChildAt(i));
					categoryTitle.setBackgroundDrawable(null);
					categoryTitle.setTextColor(0XFFADB2AD);
				}
				//设置选择单元格的背景色
				categoryTitle = (TextView) (arg0.getChildAt(arg2));
				categoryTitle.setBackgroundResource(R.drawable.categorybar_item_background);
				categoryTitle.setTextColor(0XFFFFFFFF);
			}
			
		});
		
		//把category(GridView)加入到容器中
		LinearLayout categoryLayout = (LinearLayout) findViewById(R.id.category_layout);
		//categoryLayout.setBackgroundColor(Color.RED);
		categoryLayout.addView(category);
		
		
		// 箭头
		final HorizontalScrollView categoryScrollview = (HorizontalScrollView) findViewById(R.id.category_scrollview);
		Button categoryArrowRight = (Button) findViewById(R.id.category_arrow_right);
		categoryArrowRight.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				categoryScrollview.fling(mFlingVelocityDip);
			}
		});
		
		//获取指定栏目的新闻列表
		List<HashMap<String, String>> newsData = new ArrayList<HashMap<String,String>>();
		for(int i=0;i<10;i++)
		{
			HashMap<String, String> hashMap = new HashMap<String, String>();
			hashMap.put("newslist_item_title","若水新闻客户端教程发布啦" );
			hashMap.put("newslist_item_digest","若水新闻客户端教程发布啦" );
			hashMap.put("newslist_item_source", "来源：若水工作室");
			hashMap.put("newslist_item_ptime", "2012-03-12 10:21:22");
			newsData.add(hashMap);
		}
		SimpleAdapter newsListAdapter = new SimpleAdapter(this, newsData, R.layout.newslist_item, 
										new String[]{"newslist_item_title","newslist_item_digest","newslist_item_source","newslist_item_ptime"}, 
										new int[]{R.id.newslist_item_title,R.id.newslist_item_digest,R.id.newslist_item_source,R.id.newslist_item_ptime});
		ListView newslist = (ListView)findViewById(R.id.news_list);
		newslist.setAdapter(newsListAdapter);
		
		newslist.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				Intent intent = new Intent(MainActivity.this, NewsDetailsActivity.class);
				startActivity(intent);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {

		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	/**
	 * A placeholder fragment containing a simple view.
	 */
	public static class PlaceholderFragment extends Fragment {

		public PlaceholderFragment() {
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container,
				Bundle savedInstanceState) {
			View rootView = inflater.inflate(R.layout.fragment_main, container,
					false);
			return rootView;
		}
	}

}

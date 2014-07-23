package com.example.ruoshuinews.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.StrictMode;
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
import android.widget.ProgressBar;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.example.ruoshuinews.R;
import com.example.ruoshuinews.custom.CustomSimpleAdapter;
import com.example.ruoshuinews.model.Category;
import com.example.ruoshuinews.service.SyncHttp;
import com.example.ruoshuinews.util.DensityUtil;
import com.example.ruoshuinews.util.StringUtil;

public class MainActivity extends Activity {

	
	private final int COMUMNWIDTHPX = 100;	// 新闻分类的宽度
	private int mColumnWidthDip;
	
	private final int FLINGVELOCITYPX =800;	// 滚动距离
	private int mFlingVelocityDip;

	private final int SUCCESS = 0;//加载成功
	private final int NONEWS = 1;//该栏目下没有新闻
	private final int NOMORENEWS = 2;//该栏目下没有更多新闻
	private final int LOADERROR = 3;//加载失败
	
	
	private int mCid;	//新闻分类id
	private ListView mNewsList;		//放置新闻列表的ListView
	private SimpleAdapter mNewsListAdapter;
	
	private List<HashMap<String, Object>> mNewsData; //新闻列表内容List
	
	private LayoutInflater mInflater;
	
	private Button mTitlebarRefresh;	//刷新按钮
	private ProgressBar mLoadnewsProgress;	//进度条
	private Button mLoadMoreBtn;	//加载更多按钮
	
	private LoadNewsAsyncTask loadNewsAsyncTask; //异步获取http信息并更新UI
	

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.fragment_main);
		
//		if (savedInstanceState == null) {
//		getSupportFragmentManager().beginTransaction()
//				.add(R.id.container, new PlaceholderFragment()).commit();
//	}	
		//在4.0之后在主线程里面执行Http请求都会报错,所以添加下面折行代码。正式应用不推荐这样写
		if (android.os.Build.VERSION.SDK_INT > 9) {
		    StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
		    StrictMode.setThreadPolicy(policy);}
		
		//初始化LayoutInflater
		mInflater = getLayoutInflater();
		
		//将px转换为dp
		mColumnWidthDip = DensityUtil.px2dip(this, COMUMNWIDTHPX);
		mFlingVelocityDip = DensityUtil.px2dip(this, FLINGVELOCITYPX);
		
		//初始化新闻列表
		mNewsData = new ArrayList<HashMap<String,Object>>();
		
		//获得更新按钮
		mTitlebarRefresh = (Button)findViewById(R.id.titlebar_refresh);
		mTitlebarRefresh.setOnClickListener(loadMoreListener);
		//获得进度条
		mLoadnewsProgress = (ProgressBar)findViewById(R.id.loadnews_progress);
		
		//新闻分类
		String[] categoryArray = getResources().getStringArray(R.array.categories);
		//把新闻分类保存到List中
//		List<HashMap<String, String>> categories = new ArrayList<HashMap<String, String>>();
//		for (int i = 0; i < categoryArray.length; i++) {
//			HashMap<String, String> hashMap = new HashMap<String, String>();
//			hashMap.put("category_title", categoryArray[i]);
//			categories.add(hashMap);
//		}
		
		//分割新闻类型字符串
		final List<HashMap<String, Category>> categories = new ArrayList<HashMap<String, Category>>();
		for(int i=0;i<categoryArray.length;i++)
		{
			String[] temp = categoryArray[i].split("[|]");
			if (temp.length==2)
			{
				//字符串转为int
				int cid = StringUtil.String2Int(temp[0]);
				String title = temp[1];
				Category type = new Category(cid, title);
				HashMap<String, Category> hashMap = new HashMap<String, Category>();
				hashMap.put("category_title", type);
				categories.add(hashMap);
			}
		}
		
		//默认选中的新闻分类
		mCid = 1;
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
			public void onItemClick(AdapterView<?> parent, View view, int position, long id){
				TextView categoryTitle;
				//恢复每个单元格背景色
				for (int i = 0; i < parent.getChildCount(); i++)
				{
					categoryTitle = (TextView) (parent.getChildAt(i));
					categoryTitle.setBackgroundDrawable(null);
					categoryTitle.setTextColor(0XFFADB2AD);
				}
				//设置选择单元格的背景色
				categoryTitle = (TextView) (parent.getChildAt(position));
				categoryTitle.setBackgroundResource(R.drawable.categorybar_item_background);
				categoryTitle.setTextColor(0XFFFFFFFF);
				
				//获取选中的新闻分类id
				mCid = categories.get(position).get("category_title").getCid();
//				getSpeCateNews(mCid,mNewsData,0,true);
//				//通知ListView进行更新
//				mNewsListAdapter.notifyDataSetChanged();
				loadNewsAsyncTask = new LoadNewsAsyncTask();
				loadNewsAsyncTask.execute(mCid,0,true);
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
//		List<HashMap<String, String>> newsData = new ArrayList<HashMap<String,String>>();
//		for(int i=0;i<10;i++)
//		{
//			HashMap<String, String> hashMap = new HashMap<String, String>();
//			hashMap.put("newslist_item_title","若水新闻客户端教程发布啦" );
//			hashMap.put("newslist_item_digest","若水新闻客户端教程发布啦" );
//			hashMap.put("newslist_item_source", "来源：若水工作室");
//			hashMap.put("newslist_item_ptime", "2012-03-12 10:21:22");
//			newsData.add(hashMap);
//		}
		
		//默认获取一个分类的新闻
//		getSpeCateNews(mCid,mNewsData,0,true);
		mNewsListAdapter = new SimpleAdapter(this, mNewsData, R.layout.newslist_item, 
										new String[]{"newslist_item_title","newslist_item_digest","newslist_item_source","newslist_item_ptime"}, 
										new int[]{R.id.newslist_item_title,R.id.newslist_item_digest,R.id.newslist_item_source,R.id.newslist_item_ptime});
		mNewsList = (ListView)findViewById(R.id.news_list);
		
		//创建加载更多的view
		View loadMoreLayout = mInflater.inflate(R.layout.loadmore, null);
		mNewsList.addFooterView(loadMoreLayout);	//必须放在setAdapter上面
		mNewsList.setAdapter(mNewsListAdapter);
		mNewsList.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				Intent intent = new Intent(MainActivity.this, NewsDetailsActivity.class);
				startActivity(intent);
			}
		});
		
		//找到加载更多的按钮
		mLoadMoreBtn = (Button)findViewById(R.id.loadmore_btn);
		mLoadMoreBtn.setOnClickListener(loadMoreListener);
		
		//默认获取一个分类的新闻
		loadNewsAsyncTask = new LoadNewsAsyncTask();
		loadNewsAsyncTask.execute(mCid,0,true);
	}


	
	/**
	 * 获取指定类型的新闻列表
	 * @param cid 类型ID
	 * @param newsList 保存新闻信息的集合
	 * @param startnid 分页
	 * @param firstTimes	是否第一次加载
	 */
	private int getSpeCateNews(int cid,List<HashMap<String, Object>> newsList,int startnid,Boolean firstTimes)
	{
		if (firstTimes)
		{
			//如果是第一次，则清空集合里数据
			newsList.clear();
		}
		//请求URL和字符串
		String url = "http://192.168.3.80:9292/getSpecifyCategoryNews";
		String params = "startnid="+startnid+"&count=8&cid="+cid;
		SyncHttp syncHttp = new SyncHttp();
		try
		{
			//以Get方式请求，并获得返回结果
			String retStr = syncHttp.httpGet(url, params);
			System.out.println("##"+retStr);
			JSONObject jsonObject = new JSONObject(retStr);
			//获取返回码，0表示成功
			int retCode = jsonObject.getInt("ret");
			if (0==retCode)
			{
				JSONObject dataObject = jsonObject.getJSONObject("data");
				//获取返回数目
				int totalnum = dataObject.getInt("totalnum");
				if (totalnum>0)
				{
					//获取返回新闻集合
					JSONArray newslist = dataObject.getJSONArray("newslist");
					for(int i=0;i<newslist.length();i++)
					{
						JSONObject newsObject = (JSONObject)newslist.opt(i); 
						HashMap<String, Object> hashMap = new HashMap<String, Object>();
						hashMap.put("nid", newsObject.getInt("nid"));
						hashMap.put("newslist_item_title", newsObject.getString("title"));
						hashMap.put("newslist_item_digest", newsObject.getString("digest"));
						hashMap.put("newslist_item_source", newsObject.getString("source"));
						hashMap.put("newslist_item_ptime", newsObject.getString("ptime"));
						newsList.add(hashMap);
					}
					return SUCCESS;
				}
				else
				{
					if (firstTimes)
					{
						return NONEWS;
					}
					else
					{
						return NOMORENEWS;
					}
				}
			}
			else
			{
				return LOADERROR;
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			return LOADERROR;
		}
	}

	
//	//加载更多
//	private OnClickListener loadMoreListener = new OnClickListener()
//	{
//		@Override
//		public void onClick(View v)
//		{
//			//获取该栏目下新闻
//			getSpeCateNews(mCid,mNewsData,mNewsData.size(),false);
//			//通知ListView进行更新
//			mNewsListAdapter.notifyDataSetChanged();
//		}
//	};
	//加载更多、刷新按钮的监听
	private OnClickListener loadMoreListener = new OnClickListener()
	{
		@Override
		public void onClick(View v)
		{
			loadNewsAsyncTask = new LoadNewsAsyncTask();
			switch (v.getId())
			{
			case R.id.loadmore_btn:
				//获取该栏目下新闻
				//getSpeCateNews(mCid,mNewsData,mNewsData.size(),false);
				//通知ListView进行更新
				//mNewsListAdapter.notifyDataSetChanged();
				loadNewsAsyncTask.execute(mCid,mNewsData.size(),false);
				break;
			case R.id.titlebar_refresh:
				loadNewsAsyncTask.execute(mCid,0,true);
				break;
			}
			
		}
	};	

	private class LoadNewsAsyncTask extends AsyncTask<Object, Integer, Integer>
	{
		
		@Override
		protected void onPreExecute()
		{
			//隐藏刷新按钮
			mTitlebarRefresh.setVisibility(View.GONE);
			//显示进度条
			mLoadnewsProgress.setVisibility(View.VISIBLE); 
			//设置LoadMore Button 显示文本
			mLoadMoreBtn.setText(R.string.loadmore_txt);
		}

		@Override
		protected Integer doInBackground(Object... params)
		{
			return getSpeCateNews((Integer)params[0],mNewsData,(Integer)params[1],(Boolean)params[2]);
		}

		@Override
		protected void onPostExecute(Integer result)
		{
			//根据返回值显示相关的Toast
			switch (result)
			{
			case NONEWS:
				Toast.makeText(MainActivity.this, R.string.no_news, Toast.LENGTH_LONG).show();
			break;
			case NOMORENEWS:
				Toast.makeText(MainActivity.this, R.string.no_more_news, Toast.LENGTH_LONG).show();
				break;
			case LOADERROR:
				Toast.makeText(MainActivity.this, R.string.load_news_failure, Toast.LENGTH_LONG).show();
				break;
			}
			
			mNewsListAdapter.notifyDataSetChanged();
			//显示刷新按钮
			mTitlebarRefresh.setVisibility(View.VISIBLE);
			//隐藏进度条
			mLoadnewsProgress.setVisibility(View.GONE); 
			//设置LoadMore Button 显示文本
			mLoadMoreBtn.setText(R.string.loadmore_btn);
		}
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

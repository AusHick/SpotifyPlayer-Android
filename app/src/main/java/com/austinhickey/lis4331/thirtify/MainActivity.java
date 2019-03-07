package com.austinhickey.lis4331.thirtify;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.BitmapDrawable;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.TabLayout;
import android.support.v4.widget.TextViewCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.SubMenu;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Space;
import android.widget.TableLayout;
import android.widget.TableRow;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.Album;
import kaaes.spotify.webapi.android.models.CategoriesPager;
import kaaes.spotify.webapi.android.models.Category;
import kaaes.spotify.webapi.android.models.Image;
import kaaes.spotify.webapi.android.models.Pager;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistTrack;
import kaaes.spotify.webapi.android.models.PlaylistsPager;
import kaaes.spotify.webapi.android.models.Track;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class MainActivity extends AppCompatActivity
		implements NavigationView.OnNavigationItemSelectedListener {

	private SpotifyApi api = new SpotifyApi();
	private RequestQueue mRequestQueue;
	private SpotifyService spotify;
	private JSONArray categoryJSON = new JSONArray();
	private JSONArray playlistJSON = new JSONArray();

	/*
		THIS IS THE WHOLE POINT OF THE PROJECT AND I DIDN'T
		EVEN ADD IT UNTIL 7 HOURS INTO DEVELOPMENT!
	 */
	private MediaPlayer mediaPlayer;

	//Idfk what we're doing
	public Context getContext() { return this; }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(toolbar);

		FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
		fab.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				if(mediaPlayer.isPlaying())
				{
					mediaPlayer.pause();
					fab.setImageResource(R.drawable.ic_play_arrow_black_24dp);
					Snackbar.make(view, "Music paused. FAB show/hide is breaks icon in current API.", Snackbar.LENGTH_LONG)
							.setAction("Action", null).show();
				}else{
					mediaPlayer.start();
					fab.setImageResource(R.drawable.ic_pause_black_24dp);
					Snackbar.make(view, "Music resumed. FAB show/hide is breaks icon in current API.", Snackbar.LENGTH_LONG)
							.setAction("Action", null).show();
				}
			}
		});

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
				this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.addDrawerListener(toggle);
		toggle.syncState();

		final NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
		navigationView.setNavigationItemSelectedListener(this);

		/*
			STREAM MP3 DATA FROM SPOTIFY BECAUSE WE AREN'T BARBARIANS
		 */
		mediaPlayer = new MediaPlayer();

		mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
				.setUsage(AudioAttributes.USAGE_MEDIA)
				.setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
		.build());

		mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
			@Override
			public void onCompletion(MediaPlayer mp) {
				fab.hide();
				toolbar.setTitle(R.string.app_name);
				toolbar.setSubtitle("");
			}
		});

		mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
			@Override
			public void onPrepared(MediaPlayer mp) {
				fab.show();
			}
		});

		api.setAccessToken(getIntent().getStringExtra("Token"));
		spotify = api.getService();

		try {
			categoryJSON = new JSONArray(getIntent().getStringExtra("CategoryJSON"));
		} catch (JSONException e) {
			Log.e("CategoryJSON", "Failed to parse intent");
			e.printStackTrace();
		}

		/*
			I HATE NETWORK THREADING!
		 */
		mRequestQueue = new Volley().newRequestQueue(this);

		populateNavigationMenu(navigationView.getMenu());

		populateTracks("37i9dQZF1DXcBWIGoYBM5M",50);
	}

	@Override
	public void onBackPressed() {
		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		/*
			THIS WAS A COLOSSAL WASTE OF TIME
			ENJOY YOUR CLUTTERED SIDE MENU
		 */
		/*
		for(int i = 0; i < playlistJSON.length(); i++) {
			try {
				JSONObject playlist = playlistJSON.getJSONObject(i);
				MenuItem playlistMenuItem = menu.add(playlist.getString("name"));

				playlistMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						try {
							populateTracks(playlist.getString("id"), 50);
						} catch (JSONException e) {
							e.printStackTrace();
						}

						return false;
					}
				});
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}*/

		return super.onPrepareOptionsMenu(menu);
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

		//noinspection SimplifiableIfStatement
		if (id == R.id.action_settings) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("StatementWithEmptyBody")
	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		// Handle navigation view item clicks here.
		int id = item.getItemId();

		DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
		drawer.closeDrawer(GravityCompat.START);
		return true;
	}

	private void populateNavigationMenu(Menu menu) {
		menu.clear();
		//SubMenu catSubMenu = menu.addSubMenu("Pick a category");
		try {
			for(int i = 0; i < categoryJSON.length(); i++) {
				JSONObject cat = categoryJSON.getJSONObject(i);
				SubMenu catSubMenu = menu.addSubMenu(cat.getString("name"));
				/*MenuItem catMenuItem = catSubMenu.add(cat.getString("name"));

				catMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
					@Override
					public boolean onMenuItemClick(MenuItem item) {
						try {
							playlistJSON = cat.getJSONArray("playlists");
						} catch (JSONException e) {
							e.printStackTrace();
						}
						return false;
					}
				});*/

				for(int j = 0; j < cat.getJSONArray("playlists").length(); j++) {
					JSONObject playlist = cat.getJSONArray("playlists").getJSONObject(j);
					final MenuItem playlistMenuItem = catSubMenu.add(playlist.getString("name"));
					playlistMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
						@Override
						public boolean onMenuItemClick(MenuItem item) {
							try {
								populateTracks(playlist.getString("id"), 50);
							} catch (JSONException e) {
								Log.e("PlaylistPopulate", e.toString());
							}

							return false;
						}
					});
				}
			}
		} catch (JSONException e) {
			Log.e("CategoryJSON", e.toString());
			e.printStackTrace();
		}

		return;
	}

	private void populateTracks(String playlistID, int limit) {
		final LinearLayout layoutSongs = findViewById(R.id.layoutSongs);
		layoutSongs.removeAllViews();

		final Map<String,Object> o1 = new HashMap<>();
		o1.put("limit",50);
		spotify.getPlaylistTracks("", playlistID, o1, new Callback<Pager<PlaylistTrack>>() {
			@Override
			public void success(Pager<PlaylistTrack> playlistTrackPager, Response response_garbage) {
				final int gridWidth = 2;
				LinearLayout currRow = new LinearLayout(getContext());
				for(int i = 0; i < playlistTrackPager.items.size(); i++)
				{
					if(i%gridWidth == 0)
					{
						currRow = new LinearLayout(getContext());
						currRow.setOrientation(LinearLayout.HORIZONTAL);
						LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
						currRow.setLayoutParams(lp);
						layoutSongs.addView(currRow);
					}

					Track t = playlistTrackPager.items.get(i).track;

					Image bestImg = null;
					for (Image img : t.album.images) {
						if(bestImg == null || bestImg.width < img.width)
							bestImg = img;
					}

					final Button x = new Button(getContext());

					//TableRow.LayoutParams xlp = new TableRow.LayoutParams(500,500);
					LinearLayout.LayoutParams xlp = new LinearLayout.LayoutParams(500,LinearLayout.LayoutParams.WRAP_CONTENT);
					//xlp.leftMargin = 16;
					//xlp.rightMargin = 16;
					x.setLayoutParams(xlp);

					//x.setText(t.name + "\n\n" + t.artists.stream().map(n -> n.name).collect(Collectors.joining(", ")));
					//x.setText(t.name + "\n\n" + t.artists.get(0).name);
					x.setText(t.name);
					x.setTextColor((t.preview_url != null) ? Color.WHITE : Color.RED);
					x.setShadowLayer(3,0,0,Color.BLACK);

					x.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							if(t.preview_url != null) {
								try {
									mediaPlayer.stop();
									mediaPlayer.reset();
									mediaPlayer.setDataSource(t.preview_url);
									mediaPlayer.prepare();
									mediaPlayer.start();
									((Toolbar)findViewById(R.id.toolbar)).setTitle(t.name);
									((Toolbar)findViewById(R.id.toolbar)).setSubtitle(t.artists.stream().map(n -> n.name).collect(Collectors.joining(", ")));
								} catch (IOException | IllegalStateException e) {
									Log.e("MP", e.toString());
									e.printStackTrace();
								}
							} else {
								Snackbar.make(v, "This song does not have a preview available.", Snackbar.LENGTH_LONG)
										.setAction("Action", null).show();
							}
						}
					});

					ImageRequest im = new ImageRequest(bestImg.url, response -> {
						BitmapDrawable bd = new BitmapDrawable(getResources(), response);
						bd.setColorFilter(new PorterDuffColorFilter(0xA0000000, PorterDuff.Mode.DARKEN));
						x.setBackground(bd.getCurrent());
					}, 500, 500, ImageView.ScaleType.FIT_CENTER, null, error -> Log.e("Album Art", error.toString()));

					mRequestQueue.add(im);

					currRow.addView(x);

					if(i%gridWidth < gridWidth-1)
					{
						Space s = new Space(getContext());
						TableRow.LayoutParams slp = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, 1.0f);
						s.setLayoutParams(slp);
						currRow.addView(s);
					}
				}
			}

			@Override
			public void failure(RetrofitError error) {
				Log.e("IDKWHAT", error.toString());
			}
		});
	}
}

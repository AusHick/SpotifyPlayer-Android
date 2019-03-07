package com.austinhickey.lis4331.thirtify;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import kaaes.spotify.webapi.android.SpotifyApi;
import kaaes.spotify.webapi.android.SpotifyService;
import kaaes.spotify.webapi.android.models.CategoriesPager;
import kaaes.spotify.webapi.android.models.Category;
import kaaes.spotify.webapi.android.models.PlaylistSimple;
import kaaes.spotify.webapi.android.models.PlaylistsPager;
import retrofit.Callback;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class SplashActivity extends AppCompatActivity {
	private SpotifyApi api = new SpotifyApi();
	private SpotifyService spotify;
	private String token = "BQDlgMZoQ2vkDMrTQ3z1BKvfttMEyX5EH7L9vdORKhLzHI96vAvJsRtnecAgCAm_21o_k2Q-D2FrkNc1JrPedFJ-cxhmmqz70oUoinUI5z6cHDDcXLwqsy_Qsk5QRXOsLDXq3mrQcmU72A_soUPhKSDIWLTwiLCTR7k_0w8BPZ6kjwzR26NV-yLmcUVfu6URr4AT_biV3CYkmSQXFO5XpRVVwHFJuxlqpM2fT2d8EWxsDSdv996jDEixZG2NI9ivwe0rxgdIwm6dzCM";
	private int maxRequests = 0;
	private int completedRequests = 0;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		final TextView statusText = findViewById(R.id.textSplashStatus);
		final ProgressBar progressBar = findViewById(R.id.progressBar);
		final Intent intent = new Intent(this, MainActivity.class);
		final JSONArray catData = new JSONArray();

		api.setAccessToken(token);
		spotify = api.getService();

		final Map<String,Object> o2 = new HashMap<>();
		o2.put("offset",0);
		o2.put("limit",50);
		o2.put("country","US");
		o2.put("locale","en_US");
		spotify.getCategories(o2, new Callback<CategoriesPager>() {
			@Override
			public void success(CategoriesPager categoriesPager, Response response_garbage) {
				try {
					for (Category c : categoriesPager.categories.items) {
						progressBar.setMax(++maxRequests);
						statusText.setText("Getting " + c.name + " category");

						final JSONObject catInfo = new JSONObject();
						catInfo.put("id",c.id);
						catInfo.put("name",c.name);
						catData.put(catInfo);

						final Map<String,Object> o3 = new HashMap<>();
						o3.put("limit",5);
						o3.put("country","US");
						spotify.getPlaylistsForCategory(c.id, o3, new Callback<PlaylistsPager>() {
							@Override
							public void success(PlaylistsPager playlistsPager, Response response) {
								progressBar.setProgress(++completedRequests);
								final JSONArray playlists = new JSONArray();
								try {
									for (PlaylistSimple p : playlistsPager.playlists.items) {
										statusText.setText("Getting " + p.name + " playlist");

										final JSONObject playlist = new JSONObject();
										playlist.put("id", p.id);
										playlist.put("name", p.name);
										playlists.put(playlist);
									}
									catInfo.put("playlists", playlists);

									if(completedRequests == maxRequests)
									{
										intent.putExtra("Token",token);
										intent.putExtra("CategoryJSON",catData.toString());
										startActivity(intent);

									}
								} catch (JSONException e) {
									Log.e("JSONPack", "Colossal fuck up 2");
								}
							}

							@Override
							public void failure(RetrofitError error) {
								statusText.setText(error.toString());
							}
						});
					}
				} catch (JSONException e) {
					statusText.setText(e.toString());
				}
			}

			@Override
			public void failure(RetrofitError error) {
				if(error.getResponse().getStatus() == 401)
					statusText.setText("Invalid user token, update code because we're lazy af");
				else
					statusText.setText(error.toString());
			}
		});
	}
}

package com.example.yelpnearbyapp;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

public class RestaurentDetailActivity extends AppCompatActivity {

    ImageView ivRestaurant;

    TextView tvName;
    TextView tvRating;
    TextView tvAddress;
    TextView tvPhone;
    TextView tvNoImageFound;
    TextView tvCategory;
    TextView tvReviewCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.restaurant_detail);

        ivRestaurant = (ImageView) findViewById(R.id.res_image);

        tvName = (TextView) findViewById(R.id.restaurant_name);
        tvAddress = (TextView) findViewById(R.id.restaurant_address);
        tvRating = (TextView) findViewById(R.id.rating_point);
        tvPhone = (TextView) findViewById(R.id.phno);
        tvNoImageFound = (TextView) findViewById(R.id.no_img);
        tvCategory = (TextView) findViewById(R.id.category);
        tvReviewCount = (TextView) findViewById(R.id.review_count);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle(getIntent().getStringExtra("title"));

        if (!getIntent().getStringExtra("resImg").equals("")) {
            tvNoImageFound.setVisibility(View.GONE);
            Glide.with(RestaurentDetailActivity.this)
                    .load(getIntent().getStringExtra("resImg"))
                    .into(ivRestaurant);
        } else {
            tvNoImageFound.setVisibility(View.VISIBLE);
        }

        tvName.setText(getIntent().getStringExtra("title"));
        tvAddress.setText(getIntent().getStringExtra("address").replaceAll(", $", "").trim());
        tvRating.setText(getIntent().getStringExtra("rating") + "/5");
        tvCategory.setText(getIntent().getStringExtra("category"));
        if (getIntent().getStringExtra("phone").equals("")) {
            tvPhone.setText("Not available");
        } else {
            tvPhone.setText(getIntent().getStringExtra("phone"));
        }
        tvReviewCount.setText("(" + getIntent().getStringExtra("reviewCount") + " reviews)");
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}

package com.dragon.travelmantics;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

public class DealActivity extends AppCompatActivity {

    private FirebaseDatabase mFirebaseDatabase;
    private DatabaseReference mDatabaseReference;
    private static final int PICTURE_RESULT = 42;
    EditText txtTitle;
    EditText txtDescription;
    EditText txtPrice;
    ProgressBar mProgressBar;
    ImageView imageView;
    Button imageButton;
    TravelDeal  deal;
    boolean imageSelected = false;
    String imagePath = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deal);
        FirebaseUtil.openFbReference("traveldeals", null);
        mFirebaseDatabase = FirebaseUtil.mFirebaseDatabase;
        mDatabaseReference = FirebaseUtil.mDatabaseReference;
        txtTitle = (EditText)findViewById(R.id.txtTitle);
        txtDescription = (EditText)findViewById(R.id.txtDescription);
        txtPrice = (EditText)findViewById(R.id.txtPrice);
        imageView = (ImageView)findViewById(R.id.image);
        imageButton = (Button)findViewById(R.id.btnImage);
        mProgressBar = (ProgressBar)findViewById(R.id.progressBar);
        TravelDeal deal = (TravelDeal) getIntent().getSerializableExtra("Deal");
        if(deal == null)
        {
            deal = new TravelDeal();
        }
        this.deal = deal;
        txtTitle.setText(deal.getTitle());
        txtDescription.setText(deal.getDescription());
        txtPrice.setText(deal.getPrice());
        showImage(deal.getImageUrl());
        imagePath = deal.getImageName();
        Button btnImage = (Button)findViewById(R.id.btnImage);
        btnImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                mProgressBar.setVisibility(View.VISIBLE);
                imageButton.setEnabled(false);
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(intent.createChooser(intent,"Insert Picture"),PICTURE_RESULT);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == PICTURE_RESULT && resultCode == RESULT_OK)
        {
            Uri imageUri = data.getData();
            StorageReference ref = FirebaseUtil.mStorageRef.child(imageUri.getLastPathSegment());
            ref.putFile(imageUri).addOnSuccessListener(this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(final UploadTask.TaskSnapshot taskSnapshot) {
                    if(imagePath != null)
                    {
                        deleteImage();
                    }

                    taskSnapshot.getStorage().getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            String pictureName = taskSnapshot.getStorage().getPath();
                            deal.setImageUrl(uri.toString());
                            deal.setImageName(pictureName);
                            imageSelected = true;
                            showImage(uri.toString());
                        }
                    });


                }


            });


        }
        else
        {
            mProgressBar.setVisibility(View.INVISIBLE);
            imageButton.setEnabled(true);
        }


    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.save_menu:
                if(checkData() == true)
                {
                    try
                    {
                        saveDeal();
                    }catch (Exception ex)
                    {
                        Toast.makeText(this,ex.getMessage(),Toast.LENGTH_LONG).show();
                    }

                    Toast.makeText(this,"Deal saved", Toast.LENGTH_LONG).show();
                    clean();
                    backToList();


                }
                return true;
            case R.id.delete_menu:
                deleteDeal();
                Toast.makeText(this,"Deal Deleted", Toast.LENGTH_LONG).show();
                backToList();
                return true;
           default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onCreateOptionsMenu(Menu menu)
    {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.save_menu, menu);
        if(FirebaseUtil.isAdmin){
            menu.findItem(R.id.delete_menu).setVisible(true);
            menu.findItem(R.id.save_menu).setVisible(true);
            enableEditText(true);
        }
        else
        {
            menu.findItem(R.id.delete_menu).setVisible(false);
            menu.findItem(R.id.save_menu).setVisible(false);
            enableEditText(false);
        }
        return true;
    }



    private void saveDeal(){


        deal.setTitle(txtTitle.getText().toString());
        deal.setDescription(txtDescription.getText().toString());
        deal.setPrice(txtPrice.getText().toString());
        if(deal.getId() == null)
        {
            mDatabaseReference.push().setValue(deal);
        }
        else
        {
            mDatabaseReference.child(deal.getId()).setValue(deal);
        }

    }

    private void deleteDeal()
    {
        if(deal == null)
        {
            Toast.makeText(this,"Please save the deal before deleting",Toast.LENGTH_LONG);
            return;
        }
        mDatabaseReference.child(deal.getId()).removeValue();
        if(deal.getImageName() != null && deal.getImageName().isEmpty() == false)
        {
            deleteImage();
        }
    }

    private void deleteImage()
    {
        StorageReference picRef = FirebaseUtil.mStorage.getReference().child(deal.getImageName());
        picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d("Deleted","Image Deleted");
            }
        }).addOnFailureListener(new OnFailureListener(){
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d("Deleted","An Error occured");
            }
        });
    }

    private void backToList(){
        Intent intent = new Intent(this, ListActivity.class);
        startActivity(intent);
    }


    private void clean(){
        txtTitle.setText("");
        txtDescription.setText("");;
        txtPrice.setText("");
        txtPrice.requestFocus();

    }

    private void enableEditText(boolean isEnabled)
    {
        txtTitle.setEnabled(isEnabled);
        txtDescription.setEnabled(isEnabled);
        txtPrice.setEnabled(isEnabled);
        imageButton.setEnabled(isEnabled);
    }

    private void showImage(String url)
    {
        if(url != null && !url.isEmpty())
        {
            int width = Resources.getSystem().getDisplayMetrics().widthPixels;
            Picasso.get().load(url).resize(width,width*2/3).centerCrop().into(imageView);
            mProgressBar.setVisibility(View.INVISIBLE);
            imageButton.setEnabled(true);
        }
    }

    private boolean checkData()
    {
        String description = txtDescription.getText().toString();
        String title = txtTitle.getText().toString();
        String price = txtPrice.getText().toString();


        if(title == null || title.isEmpty())
        {
            Toast.makeText(this,"Please enter deal title",Toast.LENGTH_LONG).show();
            return false;
        }
        if(price == null || price.isEmpty())
        {
            Toast.makeText(this,"Please enter deal price",Toast.LENGTH_LONG).show();
            return false;
        }
        if(description == null || description.isEmpty())
        {
            Toast.makeText(this,"Please enter deal description",Toast.LENGTH_LONG).show();
            return false;
        }
        if(!imageSelected && imagePath == null)
        {
            Toast.makeText(this,"Please select an image for the deal",Toast.LENGTH_LONG).show();
            return false;
        }
        return true;

    }
}

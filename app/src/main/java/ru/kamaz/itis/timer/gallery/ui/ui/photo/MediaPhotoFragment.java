package ru.kamaz.itis.timer.gallery.ui.ui.photo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

import android.os.Environment;
import android.provider.MediaStore;

import android.support.v4.app.Fragment;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;


//import androidx.fragment.app.Fragment;
//import androidx.recyclerview.widget.GridLayoutManager;
//import androidx.recyclerview.widget.RecyclerView;



import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import ru.kamaz.itis.timer.R;
import ru.kamaz.itis.timer.gallery.HelperUtils;
import ru.kamaz.itis.timer.gallery.constants.ConstantUtils;
import ru.kamaz.itis.timer.gallery.domain.domain.Photo;
import ru.kamaz.itis.timer.gallery.domain.interfaces.photo.MediaPhotoInterface;
import ru.kamaz.itis.timer.gallery.presentation.MediaScannerBroadcast;
import ru.kamaz.itis.timer.gallery.presentation.PhotoViewActivity;
import ru.kamaz.itis.timer.gallery.presentation.presenters.photo.MediaPhotoPresenter;
import ru.kamaz.itis.timer.gallery.ui.ui.utils.FileDeleteUtils;

public class MediaPhotoFragment extends Fragment implements MediaScannerBroadcast.MediaScannerBroadcastListener, MediaPhotoInterface.View {

    private static MediaPhotoFragment instance;

    @BindView(R.id.rv_image_gallery)
    RecyclerView rvImageGallery;

 /*   @BindView(R.id.button_update)
    Button btnUpdate;*/
    Unbinder unbinder;

    private MediaScannerBroadcast broadcast;
    private Context context;
    MediaPhotoInterface.Presenter presenter;
    List<Photo> galleryList;
    PhotoAdapter photoAdapter;
    SparseBooleanArray selectedPhotoItems;

    @SuppressLint("ValidFragment")


 public static MediaPhotoFragment newInstance() {
    if (instance == null) {
      instance = new MediaPhotoFragment();
    }
    return instance;
  }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_media_photo, container, false);
        unbinder = ButterKnife.bind(this, view);
        presenter = new MediaPhotoPresenter();
        presenter.setView(this);
        presenter.init();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_MEDIA_SCANNER_FINISHED);
        intentFilter.addDataScheme("file");
        intentFilter.addDataScheme("content");
        context.registerReceiver(broadcast, intentFilter);
        presenter.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        context.unregisterReceiver(broadcast);
        presenter.onPause();
    }

    public void removeListener() {
        presenter.removeListener();
    }

    public void addListener() {
        presenter.addListener();
    }


    @Override
    public void setListeners() {

    }

    public static ArrayList<Photo> getAllImages(Activity activity) {
        int i = 0;
        Cursor cursor = null;
        ArrayList<Photo> arrayList = new ArrayList<>();
        ContentResolver resolver = activity.getContentResolver();
        cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                null,
                null,
                null,
                null);
        if (cursor != null) {
            while (i < cursor.getCount()) {
                cursor.moveToPosition(i);
                int fieldIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
                Long id = cursor.getLong(fieldIndex);
                Uri imageUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                i++;
            }
            cursor.close();
        }

        return arrayList;
    }

    public void initVars() {
        broadcast = new MediaScannerBroadcast(this::mediaScannerBroadcastCallback);
        rvImageGallery.setHasFixedSize(true);
        RecyclerView.LayoutManager layoutManager = new GridLayoutManager(context, 7);
        rvImageGallery.setLayoutManager(layoutManager);

        selectedPhotoItems = new SparseBooleanArray();
        selectedPhotoItems.delete(0);

        galleryList = new ArrayList<>();
        //photoAdapter = new PhotoAdapter(context, galleryList, presenter);
        //rvImageGallery.setAdapter(photoAdapter);
        rvImageGallery.setLayoutManager(new GridLayoutManager(context, 4));
    }

    @Override
    public void openPhotoInFullScreen(int itemID) {
        Intent intent = new Intent(context, PhotoViewActivity.class);
        intent.putExtra(ConstantUtils.BUNDLE_PHOTO_ID, galleryList.get(itemID).getPhotoId());
        intent.putExtra(ConstantUtils.BUNDLE_PHOTOS_LIST, (Serializable) galleryList);
        intent.putExtra(ConstantUtils.BUNDLE_LIST_SELECT_POSITION, itemID);
        context.startActivity(intent);
    }

    @Override
    public void updatePhotoList() {
        selectedPhotoItems.clear();
        selectedPhotoItems.delete(0);
        photoAdapter.notifyDataSetChanged();
    }

    @Override
    public void onPhotoCheckedChanged(int itemID, boolean isChecked) {
        if (selectedPhotoItems.get(itemID, false))
            selectedPhotoItems.delete(itemID);
        else
            selectedPhotoItems.put(itemID, true);

        if (selectedPhotoItems.size() == 0)
            presenter.enableEditMode(false);
    }

    @Override
    public void shareSelectedItems() {
        List<String> selectedItems = getSelectedItems();

        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("image/*");

        for (String item : selectedItems) {
            shareIntent.putExtra(Intent.EXTRA_STREAM, HelperUtils.getUri(context, item));
        }

        startActivity(Intent.createChooser(shareIntent, getResources().getText(R.string.send_to)));

        presenter.enableEditMode(false);
    }

    @Override
    public void deleteSelectedItems() throws IOException {
        List<String> selectedItems = getSelectedItems();

        Thread thread = new Thread(new FileDeleteUtils(selectedItems, presenter, context));
        thread.start();

        presenter.enableEditMode(false);
    }

    @Override
    public void updateGalleryList() {
        galleryList.clear();
        galleryList = HelperUtils.getPhotoList(context, null);
        //galleryList = HelperUtils.getPhotoList(context, null);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        photoAdapter.updateData(galleryList);
    }

    @Override
    public void showToast(String message) {
        Toast toast = new Toast(context);
        toast.setText(message);
        toast.show();
    }

    @Override
    public void onFileDeleted(String filePath) {
        for (Photo photo : galleryList)
            if (photo.getImagePath().equals(filePath)) {
                galleryList.remove(photo);
                photoAdapter.notifyDataSetChanged();
            }
    }

    private List<String> getSelectedItems() {
        List<String> selectedItems = new ArrayList<>();

        for (int i = 0; i < galleryList.size(); i++) {
            if (selectedPhotoItems.get(i, false))
                selectedItems.add(galleryList.get(i).getImagePath());
        }

        return selectedItems;
    }

    @Override
    public void initContent() {
        if(getArguments() != null){
            long bucketId = getArguments().getLong(ConstantUtils.BUNDLE_ALBUM_ID);
            List<Photo> list = HelperUtils.getPhotoList(context, bucketId);
            photoAdapter = new PhotoAdapter(context, list, presenter);
        }
        else {
            List<Photo> list = HelperUtils.getPhotoList(context, null);
            photoAdapter = new PhotoAdapter(context, list,presenter);
        }
        rvImageGallery.setAdapter(photoAdapter);
    }

    @Override
    public void mediaScannerBroadcastCallback() {
        initContent();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        unbinder.unbind();
    }


}
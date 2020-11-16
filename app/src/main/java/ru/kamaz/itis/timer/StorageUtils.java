package ru.kamaz.itis.timer;

import android.Manifest;
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.system.Os;
import android.system.StructStatVfs;
import android.util.Log;

//import androidx.core.content.ContextCompat;




import java.io.File;
import java.io.FileNotFoundException;
import java.util.Locale;

class StorageUtils {
    private final Context context;
    private static final String TAG = "StorageUtils";
    private final MyApplicationInterface applicationInterface;
    private Uri last_media_scanned;
    private final static File base_folder = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);


    StorageUtils(Context context, MyApplicationInterface applicationInterface) {
        this.context = context;
        this.applicationInterface = applicationInterface;
    }
    static class Media {
        final long id;
        final boolean video;
        final Uri uri;
        final long date;
        final int orientation;
        final String path;

        Media(long id, boolean video, Uri uri, long date, int orientation, String path) {
            this.id = id;
            this.video = video;
            this.uri = uri;
            this.date = date;
            this.orientation = orientation;
            this.path = path;
        }
    }
    boolean isUsingSAF() {
        // check Android version just to be safe
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
            if( sharedPreferences.getBoolean(PreferenceKeys.getUsingSAFPreferenceKey(), false) ) {
                return true;
            }
        }
        return false;
    }
    private Uri getTreeUriSAF() {
        String folder_name = getSaveLocationSAF();
        return Uri.parse(folder_name);
    }
    String getSaveLocation() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PreferenceKeys.getSaveLocationPreferenceKey(), "OpenCamera");
    }
    String getSaveLocationSAF() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString(PreferenceKeys.getSaveLocationSAFPreferenceKey(), "");
    }
    private String getDataColumn(Uri uri, String selection, String [] selectionArgs) {
        final String column = "_data";
        final String[] projection = {
                column
        };

        Cursor cursor = null;
        try {
            cursor = this.context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        }
        catch(IllegalArgumentException e) {
            e.printStackTrace();
        }
        catch(SecurityException e) {
            // have received crashes from Google Play for this
            e.printStackTrace();
        }
        finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }
    void clearLastMediaScanned() {
        last_media_scanned = null;
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    File getImageFolder() {
        File file;
        if( isUsingSAF() ) {
            Uri uri = getTreeUriSAF();
    		/*if( MyDebug.LOG )
    			Log.d(TAG, "uri: " + uri);*/
            file = getFileFromDocumentUriSAF(uri, true);
        }
        else {
            String folder_name = getSaveLocation();
            file = getImageFolder(folder_name);
        }
        return file;
    }

    private static File getImageFolder(String folder_name) {
        File file;
        if( folder_name.length() > 0 && folder_name.lastIndexOf('/') == folder_name.length()-1 ) {
            // ignore final '/' character
            folder_name = folder_name.substring(0, folder_name.length()-1);
        }
        //if( folder_name.contains("/") ) {
        if( folder_name.startsWith("/") ) {
            file = new File(folder_name);
        }
        else {
            file = new File(getBaseFolder(), folder_name);
        }
        return file;
    }

    // only valid if isUsingSAF()
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private long freeMemorySAF() {
        Uri treeUri = applicationInterface.getStorageUtils().getTreeUriSAF();
        if( MyDebug.LOG )
            Log.d(TAG, "treeUri: " + treeUri);
        try {
            Uri docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, DocumentsContract.getTreeDocumentId(treeUri));
            if( MyDebug.LOG )
                Log.d(TAG, "docUri: " + docUri);
            ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(docUri, "r");
            if( pfd == null ) { // just in case
                Log.e(TAG, "pfd is null!");
                throw new FileNotFoundException();
            }
            if( MyDebug.LOG )
                Log.d(TAG, "read direct from SAF uri");
            StructStatVfs statFs = Os.fstatvfs(pfd.getFileDescriptor());
            long blocks = statFs.f_bavail;
            long size = statFs.f_bsize;
            return (blocks*size) / 1048576;
        }
        catch(IllegalArgumentException e) {
            // IllegalArgumentException can be thrown by DocumentsContract.getTreeDocumentId or getContentResolver().openFileDescriptor
            e.printStackTrace();
        }
        catch(FileNotFoundException e) {
            e.printStackTrace();
        }
        catch(Exception e) {
            // We actually just want to catch ErrnoException here, but that isn't available pre-Android 5, and trying to catch ErrnoException
            // means we crash on pre-Android 5 with java.lang.VerifyError when trying to create the StorageUtils class!
            // One solution might be to move this method to a separate class that's only created on Android 5+, but this is a quick fix for
            // now.
            e.printStackTrace();
        }
        return -1;
    }
    public static File getBaseFolder() {
        return base_folder;
    }


    public long freeMemory() { // return free memory in MB
        if( MyDebug.LOG )
            Log.d(TAG, "freeMemory");
        if( applicationInterface.getStorageUtils().isUsingSAF() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            // if we fail for SAF, don't fall back to the methods below, as this may be incorrect (especially for external SD card)
            return freeMemorySAF();
        }
        try {
            File folder = getImageFolder();
            if( folder == null ) {
                throw new IllegalArgumentException(); // so that we fall onto the backup
            }
            StatFs statFs = new StatFs(folder.getAbsolutePath());
            long blocks, size;
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ) {
                blocks = statFs.getAvailableBlocksLong();
                size = statFs.getBlockSizeLong();
            }
            else {
                // cast to long to avoid overflow!
                //noinspection deprecation
                blocks = statFs.getAvailableBlocks();
                //noinspection deprecation
                size = statFs.getBlockSize();
            }
            return (blocks*size) / 1048576;
        }
        catch(IllegalArgumentException e) {
            // this can happen if folder doesn't exist, or don't have read access
            // if the save folder is a subfolder of DCIM, we can just use that instead
            try {
                if( !isUsingSAF() ) {
                    // getSaveLocation() only valid if !isUsingSAF()
                    String folder_name = getSaveLocation();
                    if( !folder_name.startsWith("/") ) {
                        File folder = getBaseFolder();
                        StatFs statFs = new StatFs(folder.getAbsolutePath());
                        long blocks, size;
                        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 ) {
                            blocks = statFs.getAvailableBlocksLong();
                            size = statFs.getBlockSizeLong();
                        }
                        else {
                            // cast to long to avoid overflow!
                            //noinspection deprecation
                            blocks = statFs.getAvailableBlocks();
                            //noinspection deprecation
                            size = statFs.getBlockSize();
                        }
                        return (blocks*size) / 1048576;
                    }
                }
            }
            catch(IllegalArgumentException e2) {
                // just in case
            }
        }
        return -1;
    }
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public File getFileFromDocumentUriSAF(Uri uri, boolean is_folder) {
        if( MyDebug.LOG ) {
            Log.d(TAG, "getFileFromDocumentUriSAF: " + uri);
            Log.d(TAG, "is_folder?: " + is_folder);
        }
        String authority = uri.getAuthority();
        if( MyDebug.LOG ) {
            Log.d(TAG, "authority: " + authority);
            Log.d(TAG, "scheme: " + uri.getScheme());
            Log.d(TAG, "fragment: " + uri.getFragment());
            Log.d(TAG, "path: " + uri.getPath());
            Log.d(TAG, "last path segment: " + uri.getLastPathSegment());
        }
        File file = null;
        if( "com.android.externalstorage.documents".equals(authority) ) {
            final String id = is_folder ? DocumentsContract.getTreeDocumentId(uri) : DocumentsContract.getDocumentId(uri);
            if( MyDebug.LOG )
                Log.d(TAG, "id: " + id);
            String [] split = id.split(":");
            if( split.length >= 2 ) {
                String type = split[0];
                String path = split[1];
				/*if( MyDebug.LOG ) {
					Log.d(TAG, "type: " + type);
					Log.d(TAG, "path: " + path);
				}*/
                File [] storagePoints = new File("/storage").listFiles();

                if( "primary".equalsIgnoreCase(type) ) {
                    final File externalStorage = Environment.getExternalStorageDirectory();
                    file = new File(externalStorage, path);
                }
                for(int i=0;storagePoints != null && i<storagePoints.length && file==null;i++) {
                    File externalFile = new File(storagePoints[i], path);
                    if( externalFile.exists() ) {
                        file = externalFile;
                    }
                }
                if( file == null ) {
                    // just in case?
                    file = new File(path);
                }
            }
        }
        else if( "com.android.providers.downloads.documents".equals(authority) ) {
            if( !is_folder ) {
                final String id = DocumentsContract.getDocumentId(uri);
                if( id.startsWith("raw:") ) {
                    // unclear if this is needed for Open Camera, but on Vibrance HDR
                    // on some devices (at least on a Chromebook), I've had reports of id being of the form
                    // "raw:/storage/emulated/0/Download/..."
                    String filename = id.replaceFirst("raw:", "");
                    file = new File(filename);
                }
                else {
                    try {
                        final Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.parseLong(id));

                        String filename = getDataColumn(contentUri, null, null);
                        if( filename != null )
                            file = new File(filename);
                    }
                    catch(NumberFormatException e) {
                        // have had crashes from Google Play from Long.parseLong(id)
                        Log.e(TAG,"failed to parse id: " + id);
                        e.printStackTrace();
                    }
                }
            }
            else {
                if( MyDebug.LOG )
                    Log.d(TAG, "downloads uri not supported for folders");
                // This codepath can be reproduced by enabling SAF and selecting Downloads.
                // DocumentsContract.getDocumentId() throws IllegalArgumentException for
                // this (content://com.android.providers.downloads.documents/tree/downloads).
                // If we use DocumentsContract.getTreeDocumentId() for folders, it returns
                // "downloads" - not clear how to parse this!
            }
        }
        else if( "com.android.providers.media.documents".equals(authority) ) {
            final String docId = DocumentsContract.getDocumentId(uri);
            final String[] split = docId.split(":");
            final String type = split[0];

            Uri contentUri = null;
            switch (type) {
                case "image":
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                    break;
                case "video":
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                    break;
                case "audio":
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                    break;
            }

            final String selection = "_id=?";
            final String[] selectionArgs = new String[] {
                    split[1]
            };

            String filename = getDataColumn(contentUri, selection, selectionArgs);
            if( filename != null )
                file = new File(filename);
        }

        if( MyDebug.LOG ) {
            if( file != null )
                Log.d(TAG, "file: " + file.getAbsolutePath());
            else
                Log.d(TAG, "failed to find file");
        }
        return file;
    }


    private Media getLatestMedia(boolean video) {
        if( MyDebug.LOG )
            Log.d(TAG, "getLatestMedia: " + (video ? "video" : "images"));
        if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ) {
            if( MyDebug.LOG )
                Log.e(TAG, "don't have READ_EXTERNAL_STORAGE permission");
            return null;
        }
        Cursor cursor = null;
        Media media = null;
        Uri baseUri = video ? MediaStore.Video.Media.EXTERNAL_CONTENT_URI : MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        final int column_id_c = 0;
        final int column_date_taken_c = 1;
        final int column_data_c = 2; // full path and filename, including extension
        final int column_orientation_c = 3; // for images only
        String [] projection = video ? new String[] {MediaStore.Video.VideoColumns._ID, MediaStore.Video.VideoColumns.DATE_TAKEN, MediaStore.Video.VideoColumns.DATA} : new String[] {MediaStore.Images.ImageColumns._ID, MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.Images.ImageColumns.DATA, MediaStore.Images.ImageColumns.ORIENTATION};
        // for images, we need to search for JPEG/etc and RAW, to support RAW only mode (even if we're not currently in that mode, it may be that previously the user did take photos in RAW only mode)
        String selection = video ? "" : MediaStore.Images.ImageColumns.MIME_TYPE + "='image/jpeg' OR " +
                MediaStore.Images.ImageColumns.MIME_TYPE + "='image/webp' OR " +
                MediaStore.Images.ImageColumns.MIME_TYPE + "='image/png' OR " +
                MediaStore.Images.ImageColumns.MIME_TYPE + "='image/x-adobe-dng'";
        String order = video ? MediaStore.Video.VideoColumns.DATE_TAKEN + " DESC," + MediaStore.Video.VideoColumns._ID + " DESC" : MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC," + MediaStore.Images.ImageColumns._ID + " DESC";



        try {
            cursor = context.getContentResolver().query(baseUri, projection, selection, null, order);
            if( cursor != null && cursor.moveToFirst() ) {
                if( MyDebug.LOG )
                    Log.d(TAG, "found: " + cursor.getCount());
                // now sorted in order of date - scan to most recent one in the Open Camera save folder
                boolean found = false;
                File save_folder = getImageFolder(); // may be null if using SAF
                String save_folder_string = save_folder == null ? null : save_folder.getAbsolutePath() + File.separator;
                if( MyDebug.LOG )
                    Log.d(TAG, "save_folder_string: " + save_folder_string);
                do {
                    String path = cursor.getString(column_data_c);
                    if( MyDebug.LOG )
                        Log.d(TAG, "path: " + path);
                    // path may be null on Android 4.4!: http://stackoverflow.com/questions/3401579/get-filename-and-path-from-uri-from-mediastore
                    if( save_folder_string == null || (path != null && path.contains(save_folder_string) ) ) {
                        if( MyDebug.LOG )
                            Log.d(TAG, "found most recent in Open Camera folder");
                        // we filter files with dates in future, in case there exists an image in the folder with incorrect datestamp set to the future
                        // we allow up to 2 days in future, to avoid risk of issues to do with timezone etc
                        long date = cursor.getLong(column_date_taken_c);
                        long current_time = System.currentTimeMillis();
                        if( date > current_time + 172800000 ) {
                            if( MyDebug.LOG )
                                Log.d(TAG, "skip date in the future!");
                        }
                        else {
                            found = true;
                            break;
                        }
                    }
                }
                while( cursor.moveToNext() );
                if( found ) {
                    // make sure we prefer JPEG/etc (non RAW) if there's a JPEG/etc version of this image
                    // this is because we want to support RAW only and JPEG+RAW modes
                    String path = cursor.getString(column_data_c);
                    if( MyDebug.LOG )
                        Log.d(TAG, "path: " + path);
                    // path may be null on Android 4.4, see above!
                    if( path != null && path.toLowerCase(Locale.US).endsWith(".dng") ) {
                        if( MyDebug.LOG )
                            Log.d(TAG, "try to find a non-RAW version of the DNG");
                        int dng_pos = cursor.getPosition();
                        boolean found_non_raw = false;
                        String path_without_ext = path.toLowerCase(Locale.US);
                        if( path_without_ext.indexOf(".") > 0 )
                            path_without_ext = path_without_ext.substring(0, path_without_ext.lastIndexOf("."));
                        if( MyDebug.LOG )
                            Log.d(TAG, "path_without_ext: " + path_without_ext);
                        while( cursor.moveToNext() ) {
                            String next_path = cursor.getString(column_data_c);
                            if( MyDebug.LOG )
                                Log.d(TAG, "next_path: " + next_path);
                            if( next_path == null )
                                break;
                            String next_path_without_ext = next_path.toLowerCase(Locale.US);
                            if( next_path_without_ext.indexOf(".") > 0 )
                                next_path_without_ext = next_path_without_ext.substring(0, next_path_without_ext.lastIndexOf("."));
                            if( MyDebug.LOG )
                                Log.d(TAG, "next_path_without_ext: " + next_path_without_ext);
                            if( !path_without_ext.equals(next_path_without_ext) )
                                break;
                            // so we've found another file with matching filename - is it a JPEG/etc?
                            if( next_path.toLowerCase(Locale.US).endsWith(".jpg") ) {
                                if( MyDebug.LOG )
                                    Log.d(TAG, "found equivalent jpeg");
                                found_non_raw = true;
                                break;
                            }
                            else if( next_path.toLowerCase(Locale.US).endsWith(".webp") ) {
                                if( MyDebug.LOG )
                                    Log.d(TAG, "found equivalent webp");
                                found_non_raw = true;
                                break;
                            }
                            else if( next_path.toLowerCase(Locale.US).endsWith(".png") ) {
                                if( MyDebug.LOG )
                                    Log.d(TAG, "found equivalent png");
                                found_non_raw = true;
                                break;
                            }
                        }
                        if( !found_non_raw ) {
                            if( MyDebug.LOG )
                                Log.d(TAG, "can't find equivalent jpeg/etc");
                            cursor.moveToPosition(dng_pos);
                        }
                    }
                }
                if( !found ) {
                    if( MyDebug.LOG )
                        Log.d(TAG, "can't find suitable in Open Camera folder, so just go with most recent");
                    cursor.moveToFirst();
                }
                long id = cursor.getLong(column_id_c);
                long date = cursor.getLong(column_date_taken_c);
                int orientation = video ? 0 : cursor.getInt(column_orientation_c);
                Uri uri = ContentUris.withAppendedId(baseUri, id);
                String path = cursor.getString(column_data_c);
                if( MyDebug.LOG )
                    Log.d(TAG, "found most recent uri for " + (video ? "video" : "images") + ": " + uri);
                media = new Media(id, video, uri, date, orientation, path);
            }
        }
        catch(Exception e) {
            // have had exceptions such as SQLiteException, NullPointerException reported on Google Play from within getContentResolver().query() call
            if( MyDebug.LOG )
                Log.e(TAG, "Exception trying to find latest media");
            e.printStackTrace();
        }
        finally {
            if( cursor != null ) {
                cursor.close();
            }
        }
        return media;
    }

    Media getLatestMedia() {
        Media image_media = getLatestMedia(false);
        Media video_media = getLatestMedia(true);
        Media media = null;
        if( image_media != null && video_media == null ) {
            if( MyDebug.LOG )
                Log.d(TAG, "only found images");
            media = image_media;
        }
        else if( image_media == null && video_media != null ) {
            if( MyDebug.LOG )
                Log.d(TAG, "only found videos");
            media = video_media;
        }
        else if( image_media != null && video_media != null ) {
            if( MyDebug.LOG ) {
                Log.d(TAG, "found images and videos");
                Log.d(TAG, "latest image date: " + image_media.date);
                Log.d(TAG, "latest video date: " + video_media.date);
            }
            if( image_media.date >= video_media.date ) {
                if( MyDebug.LOG )
                    Log.d(TAG, "latest image is newer");
                media = image_media;
            }
            else {
                if( MyDebug.LOG )
                    Log.d(TAG, "latest video is newer");
                media = video_media;
            }
        }
        if( MyDebug.LOG )
            Log.d(TAG, "return latest media: " + media);
        return media;
    }

}

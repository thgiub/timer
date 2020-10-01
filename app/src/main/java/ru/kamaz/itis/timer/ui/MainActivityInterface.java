package ru.kamaz.itis.timer.ui;

public interface MainActivityInterface {
    interface View{
        void setListeners();
        void initVars();
        void updateGalleryList();


    }
    interface Presenter{
        void init();
        void onResume();
        void onPause();
        void setView(View view);
        void updateGalleryList();
    }
}

package ru.kamaz.itis.timer.presenter;

import ru.kamaz.itis.timer.gallery.domain.interactors.MainInteractor;
import ru.kamaz.itis.timer.gallery.domain.interfaces.interactors.MainInteractorInterface;
import ru.kamaz.itis.timer.ui.MainActivityInterface;

public class MainActivityPresenter implements MainActivityInterface.Presenter, MainInteractorInterface.MediaListener {
    private MainActivityInterface.View view;
    private MainInteractor interactor;
    @Override
    public void setView(MainActivityInterface.View view) {
        this.view=view;
    }


    @Override
    public void init() {
        view.initVars();
        view.setListeners();
        interactor= MainInteractor.getInstance();
    }

    @Override
    public void onResume() {
        interactor.addListener(this);
    }

    @Override
    public void onPause() {
        interactor.removeListener(this);
    }

    @Override
    public void updateGalleryList() {
        interactor.updateGalleryAfterPhoto();
    }

    @Override
    public void setEditMode(boolean enable) {

    }

    @Override
    public void shareSelectedItems() {

    }

    @Override
    public void deleteSelectedItems() {

    }

    @Override
    public void removeListener(boolean bool) {

    }

    @Override
    public void addListener(boolean bool) {
        if (false){
            interactor.listenersAdd(bool);
        }

    }

    @Override
    public void updateGallery() {

    }
}

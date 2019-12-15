package org.mediasoup.droid.demo.vm;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import org.mediasoup.droid.lib.lv.RoomStore;

import java.lang.reflect.InvocationTargetException;

public abstract class EdiasProps extends AndroidViewModel {

  @NonNull private final RoomStore mRoomStore;

  EdiasProps(@NonNull Application application, @NonNull RoomStore roomStore) {
    super(application);
    mRoomStore = roomStore;
  }

  @NonNull
  RoomStore getRoomStore() {
    return mRoomStore;
  }

  public abstract void connect(LifecycleOwner lifecycleOwner);

  /**
   * A creator is used to inject the product ID into the ViewModel
   *
   * <p>This creator is to showcase how to inject dependencies into ViewModels. It's not actually
   * necessary in this case, as the product ID can be passed in a public method.
   */
  public static class Factory extends ViewModelProvider.NewInstanceFactory {

    @NonNull private final Application mApplication;
    @NonNull private final RoomStore mStore;

    public Factory(@NonNull Application application, @NonNull RoomStore store) {
      mApplication = application;
      mStore = store;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
      if (EdiasProps.class.isAssignableFrom(modelClass)) {
        try {
          return modelClass
              .getConstructor(Application.class, RoomStore.class)
              .newInstance(mApplication, mStore);
        } catch (NoSuchMethodException e) {
          throw new RuntimeException("Cannot create an instance of " + modelClass, e);
        } catch (IllegalAccessException e) {
          throw new RuntimeException("Cannot create an instance of " + modelClass, e);
        } catch (InstantiationException e) {
          throw new RuntimeException("Cannot create an instance of " + modelClass, e);
        } catch (InvocationTargetException e) {
          throw new RuntimeException("Cannot create an instance of " + modelClass, e);
        }
      }
      return super.create(modelClass);
    }
  }
}

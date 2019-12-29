package org.mediasoup.droid.demo.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LifecycleOwner;
import androidx.recyclerview.widget.RecyclerView;

import org.mediasoup.droid.Logger;
import org.mediasoup.droid.demo.R;
import org.mediasoup.droid.demo.view.PeerView;
import org.mediasoup.droid.demo.vm.PeerProps;
import org.mediasoup.droid.lib.RoomClient;
import org.mediasoup.droid.lib.lv.RoomStore;
import org.mediasoup.droid.lib.model.Peer;

import java.util.LinkedList;
import java.util.List;

public class PeerAdapter extends RecyclerView.Adapter<PeerAdapter.PeerViewHolder> {

  private static final String TAG = "PeerAdapter";

  @NonNull private RoomStore mStore;
  @NonNull private LifecycleOwner mLifecycleOwner;
  @NonNull private RoomClient mRoomClient;

  private List<Peer> mPeers = new LinkedList<>();

  private int containerHeight;

  public PeerAdapter(
      @NonNull RoomStore store,
      @NonNull LifecycleOwner lifecycleOwner,
      @NonNull RoomClient roomClient) {
    mStore = store;
    mLifecycleOwner = lifecycleOwner;
    mRoomClient = roomClient;
  }

  public void replacePeers(@NonNull List<Peer> peers) {
    mPeers = peers;
    notifyDataSetChanged();
  }

  @NonNull
  @Override
  public PeerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    containerHeight = parent.getHeight();
    Context context = parent.getContext();
    View view = LayoutInflater.from(context).inflate(R.layout.item_remote_peer, parent, false);
    return new PeerViewHolder(
        view, new PeerProps(((AppCompatActivity) context).getApplication(), mStore));
  }

  @Override
  public void onBindViewHolder(@NonNull PeerViewHolder holder, int position) {
    // update height
    ViewGroup.LayoutParams layoutParams = holder.mPeerView.getLayoutParams();
    layoutParams.height = getItemHeight();
    holder.mPeerView.setLayoutParams(layoutParams);
    // bind
    holder.bind(mLifecycleOwner, mRoomClient, mPeers.get(position));
  }

  @Override
  public int getItemCount() {
    return mPeers.size();
  }

  private int getItemHeight() {
    int itemCount = getItemCount();
    if (itemCount <= 1) {
      return containerHeight;
    } else if (itemCount <= 3) {
      return containerHeight / itemCount;
    } else {
      return (int) (containerHeight / 3.2);
    }
  }

  static class PeerViewHolder extends RecyclerView.ViewHolder {

    @NonNull final PeerView mPeerView;
    @NonNull final PeerProps mPeerProps;

    PeerViewHolder(@NonNull View view, @NonNull PeerProps peerProps) {
      super(view);
      mPeerView = view.findViewById(R.id.remote_peer);
      mPeerProps = peerProps;
    }

    void bind(LifecycleOwner owner, RoomClient roomClient, @NonNull Peer peer) {
      Logger.d(TAG, "bind() id: " + peer.getId() + ", name: " + peer.getDisplayName());
      mPeerProps.connect(owner, peer.getId());
      mPeerView.setProps(mPeerProps, roomClient);
    }
  }
}

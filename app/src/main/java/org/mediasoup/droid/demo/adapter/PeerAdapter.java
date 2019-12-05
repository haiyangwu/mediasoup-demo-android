package org.mediasoup.droid.demo.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.mediasoup.droid.demo.R;
import org.mediasoup.droid.demo.view.PeerView;
import org.mediasoup.droid.lib.model.Peer;

import java.util.LinkedList;
import java.util.List;

public class PeerAdapter extends RecyclerView.Adapter<PeerAdapter.PeerViewHolder> {

  private int containerHeight;
  private List<Peer> peers = new LinkedList<>();

  @NonNull
  @Override
  public PeerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    containerHeight = parent.getHeight();
    View view =
        LayoutInflater.from(parent.getContext()).inflate(R.layout.item_remote_peer, parent, false);
    return new PeerViewHolder(view);
  }

  public void replacePeers(List<Peer> peers) {
    this.peers.clear();
    this.peers.addAll(peers);
    notifyDataSetChanged();
  }

  @Override
  public void onBindViewHolder(@NonNull PeerViewHolder holder, int position) {
    holder.bindView(peers.get(position));
  }

  @Override
  public void onViewAttachedToWindow(@NonNull PeerViewHolder holder) {
    super.onViewAttachedToWindow(holder);
    // TODO:
  }

  @Override
  public void onViewDetachedFromWindow(@NonNull PeerViewHolder holder) {
    super.onViewDetachedFromWindow(holder);
    // TODO;
  }

  @Override
  public int getItemCount() {
    return peers.size();
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

  public class PeerViewHolder extends RecyclerView.ViewHolder {

    View itemContainer;
    PeerView pvItem;

    PeerViewHolder(@NonNull View itemView) {
      super(itemView);
      itemContainer = itemView;
      pvItem = itemView.findViewById(R.id.remote_peer);
    }

    void bindView(Peer peer) {
      pvItem.bind(peer);
      ViewGroup.LayoutParams layoutParams = itemContainer.getLayoutParams();
      layoutParams.height = getItemHeight();
      itemContainer.setLayoutParams(layoutParams);
    }
  }
}

package com.fsck.k9.activity;


import java.util.List;

import android.content.Context;
import android.graphics.PorterDuff.Mode;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.QuickContactBadge;
import android.widget.TextView;

import com.fsck.k9.R;
import com.fsck.k9.view.RecipientSelectView.Recipient;
import com.fsck.k9.view.RecipientSelectView.RecipientCryptoStatus;


public class AlternateRecipientAdapter extends BaseAdapter {

    private final Context context;
    private List<Recipient> recipients;

    private Recipient currentRecipient;
    private AlternateRecipientListener listener;

    public AlternateRecipientAdapter(Context context, AlternateRecipientListener listener) {
        super();
        this.context = context;
        this.listener = listener;
    }

    public void setCurrentRecipient(Recipient currentRecipient) {
        this.currentRecipient = currentRecipient;
    }

    public void setAlternateRecipientInfo(List<Recipient> recipients) {
        this.recipients = recipients;
        recipients.remove(currentRecipient);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        // number of alternate addresses, +1 for the header, +1 for the current address
        if (recipients == null) {
            return 2;
        }
        return recipients.size() +2;
    }

    @Override
    public Recipient getItem(int position) {
        // position 0 is the recipient we display alternates for, substitute it
        if (position == 0 || position == 1) {
            return currentRecipient;
        }
        return recipients == null ? null : recipients.get(position -2);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        if (view == null) {
            view = newView(parent);
        }
        Recipient recipient = getItem(position);
        if (position == 0) {
            bindHeaderView(view, recipient);
        } else {
            bindItemView(view, recipient);
        }

        return view;
    }

    public View newView(ViewGroup parent) {
        View view = LayoutInflater.from(context).inflate(R.layout.recipient_alternate_item, parent, false);
        RecipientTokenHolder holder = new RecipientTokenHolder(view);
        view.setTag(holder);
        return view;
    }

    @Override
    public boolean isEnabled(int position) {
        // the header isn't clickable, all other elements are
        return position > 0;
    }

    public void bindHeaderView(View view, Recipient recipient) {
        RecipientTokenHolder holder = (RecipientTokenHolder) view.getTag();
        holder.setShowAsHeader(true);

        holder.headerName.setText(recipient.getDisplayNameOrUnknown(context));

        RecipientAdapter.setContactPhotoOrPlaceholder(context, holder.headerPhoto, recipient);
        holder.headerPhoto.assignContactUri(recipient.getContactLookupUri());

        holder.headerRemove.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onRecipientRemove(currentRecipient);
            }
        });
    }

    public void bindItemView(View view, final Recipient recipient) {
        RecipientTokenHolder holder = (RecipientTokenHolder) view.getTag();
        holder.setShowAsHeader(false);

        String address = recipient.address.getAddress();
        holder.itemAddress.setText(address);
        if (!TextUtils.isEmpty(recipient.addressLabel)) {
            holder.itemAddressLabel.setText(recipient.addressLabel);
            holder.itemAddressLabel.setVisibility(View.VISIBLE);
        } else {
            holder.itemAddressLabel.setVisibility(View.GONE);
        }

        boolean isCurrent = currentRecipient == recipient;
        holder.itemAddress.setTypeface(null, isCurrent ? Typeface.BOLD : Typeface.NORMAL);
        holder.itemAddressLabel.setTypeface(null, isCurrent ? Typeface.BOLD : Typeface.NORMAL);

        holder.layoutItem.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                listener.onRecipientChange(currentRecipient, recipient);
            }
        });

        Integer cryptoStatusRes = null, cryptoStatusColor = null;
        RecipientCryptoStatus cryptoStatus = recipient.getCryptoStatus();
        switch (cryptoStatus) {
            case AVAILABLE_TRUSTED:
                cryptoStatusRes = R.drawable.status_lock_closed;
                cryptoStatusColor = context.getResources().getColor(R.color.openpgp_green);
                break;
            case AVAILABLE_UNTRUSTED:
                cryptoStatusRes = R.drawable.status_lock_error;
                cryptoStatusColor = context.getResources().getColor(R.color.openpgp_orange);
                break;
            case UNAVAILABLE:
                cryptoStatusRes = R.drawable.status_lock_open;
                cryptoStatusColor = context.getResources().getColor(R.color.openpgp_red);
                break;
        }

        if (cryptoStatusRes != null) {
            // we could do this easier with setImageTintList, but that's API level 21
            Drawable drawable = context.getResources().getDrawable(cryptoStatusRes);
            // noinspection ConstantConditions, we know the resource exists!
            drawable.mutate();
            drawable.setColorFilter(cryptoStatusColor, Mode.SRC_ATOP);
            holder.itemCryptoStatus.setImageDrawable(drawable);
            holder.itemCryptoStatus.setVisibility(View.VISIBLE);
        } else {
            holder.itemCryptoStatus.setVisibility(View.GONE);
        }

    }

    static class RecipientTokenHolder {
        View layoutHeader, layoutItem;

        TextView headerName;
        QuickContactBadge headerPhoto;
        View headerRemove;

        TextView itemAddress;
        TextView itemAddressLabel;
        ImageView itemCryptoStatus;

        public RecipientTokenHolder(View view) {
            layoutHeader = view.findViewById(R.id.alternate_container_header);
            layoutItem = view.findViewById(R.id.alternate_container_item);

            headerName = (TextView) view.findViewById(R.id.alternate_header_name);
            headerPhoto = (QuickContactBadge) view.findViewById(R.id.alternate_contact_photo);
            headerRemove = view.findViewById(R.id.alternate_remove);

            itemAddress = (TextView) view.findViewById(R.id.alternate_address);
            itemAddressLabel = (TextView) view.findViewById(R.id.alternate_address_label);
            itemCryptoStatus = (ImageView) view.findViewById(R.id.alternate_crypto_status);
        }

        public void setShowAsHeader(boolean isHeader) {
            layoutHeader.setVisibility(isHeader ? View.VISIBLE : View.GONE);
            layoutItem.setVisibility(isHeader ? View.GONE : View.VISIBLE);
        }
    }

    public interface AlternateRecipientListener {
        void onRecipientRemove(Recipient currentRecipient);
        void onRecipientChange(Recipient currentRecipient, Recipient alternateRecipient);
    }

}

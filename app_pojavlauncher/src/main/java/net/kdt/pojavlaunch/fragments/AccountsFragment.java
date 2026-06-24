package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import net.kdt.pojavlaunch.PojavApplication;
import net.kdt.pojavlaunch.Tools;
import net.kdt.pojavlaunch.authenticator.accounts.Accounts;
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount;
import net.kdt.pojavlaunch.extra.ExtraConstants;
import net.kdt.pojavlaunch.extra.ExtraCore;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import git.artdeell.mojo.R;

public class AccountsFragment extends Fragment {

    private RecyclerView mRecyclerView;
    private LinearLayout mEmptyState;
    private TextView mAccountCount;
    private AccountCardAdapter mAdapter;

    private List<MinecraftAccount> mAccounts = new ArrayList<>();
    private int mSelectedIndex = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_accounts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mRecyclerView = view.findViewById(R.id.account_recycler);
        mEmptyState = view.findViewById(R.id.empty_state);
        mAccountCount = view.findViewById(R.id.account_count);
        Button btnAdd = view.findViewById(R.id.btn_add_account);

        mAdapter = new AccountCardAdapter();
        mRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        mRecyclerView.setAdapter(mAdapter);

        btnAdd.setOnClickListener(v -> ExtraCore.setValue(ExtraConstants.SELECT_AUTH_METHOD, true));

        loadAccounts();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadAccounts();
    }

    private void loadAccounts() {
        PojavApplication.sExecutorService.execute(() -> {
            try {
                Accounts accounts = Accounts.load();
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() -> {
                    mAccounts = new ArrayList<>(accounts.accounts);
                    mSelectedIndex = accounts.selectionIndex;
                    updateUI();
                });
            } catch (IOException e) {
                if (getActivity() == null) return;
                getActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Failed to load accounts", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void updateUI() {
        if (mAccounts.isEmpty()) {
            mRecyclerView.setVisibility(View.GONE);
            mEmptyState.setVisibility(View.VISIBLE);
            mAccountCount.setText("0 accounts");
        } else {
            mRecyclerView.setVisibility(View.VISIBLE);
            mEmptyState.setVisibility(View.GONE);
            mAccountCount.setText(mAccounts.size() + " account" + (mAccounts.size() == 1 ? "" : "s"));
            mAdapter.notifyDataSetChanged();
        }
    }

    private void selectAccount(int position) {
        if (position < 0 || position >= mAccounts.size()) return;
        mSelectedIndex = position;
        Accounts.setCurrent(mAccounts.get(position));
        mAdapter.notifyDataSetChanged();
        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true);
        Toast.makeText(requireContext(), "Switched to " + mAccounts.get(position).username, Toast.LENGTH_SHORT).show();
    }

    private void deleteAccount(int position) {
        if (position < 0 || position >= mAccounts.size()) return;
        MinecraftAccount account = mAccounts.get(position);

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Remove " + account.username + "?")
                .setPositiveButton("Delete", (d, w) -> {
                    Accounts.delete(account);
                    loadAccounts();
                    ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void refreshAccount(int position) {
        if (position < 0 || position >= mAccounts.size()) return;
        MinecraftAccount account = mAccounts.get(position);
        Toast.makeText(requireContext(), "Refreshing " + account.username + "...", Toast.LENGTH_SHORT).show();
        PojavApplication.sExecutorService.execute(() -> {
            try {
                account.reload();
            } catch (Exception ignored) {}
        });
    }

    private class AccountCardAdapter extends RecyclerView.Adapter<AccountCardAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_account_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            MinecraftAccount account = mAccounts.get(position);
            boolean isSelected = position == mSelectedIndex;

            holder.username.setText(account.username);
            holder.authType.setText(account.authType != null ? account.authType.name() : "Unknown");

            // Selected state
            holder.selectedIndicator.setVisibility(isSelected ? View.VISIBLE : View.GONE);
            holder.itemView.setBackgroundResource(isSelected ? R.drawable.account_card_selected_bg : R.drawable.account_card_bg);

            // Load skin head
            if (account.getSkinFace() != null) {
                holder.avatar.setImageBitmap(account.getSkinFace());
            } else {
                holder.avatar.setImageResource(R.drawable.steve_head_front);
            }

            // Auth type badge
            if (account.authType != null && account.authType.iconResource != 0) {
                holder.badge.setImageResource(account.authType.iconResource);
                holder.badge.setVisibility(View.VISIBLE);
            } else {
                holder.badge.setVisibility(View.GONE);
            }

            // Status
            if (isSelected) {
                holder.statusBar.setVisibility(View.VISIBLE);
                long now = System.currentTimeMillis();
                if (account.expiresAt > 0 && now > account.expiresAt) {
                    holder.statusText.setText("Token expired — will refresh on use");
                } else {
                    holder.statusText.setText("Active account");
                }
            } else {
                holder.statusBar.setVisibility(View.GONE);
            }

            // Actions
            holder.btnRefresh.setOnClickListener(v -> refreshAccount(holder.getAdapterPosition()));
            holder.btnDelete.setOnClickListener(v -> deleteAccount(holder.getAdapterPosition()));
            holder.itemView.setOnClickListener(v -> selectAccount(holder.getAdapterPosition()));
        }

        @Override
        public int getItemCount() {
            return mAccounts.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            android.widget.ImageView avatar, badge, btnRefresh, btnDelete;
            TextView username, authType, statusText;
            View selectedIndicator, statusBar;

            ViewHolder(View itemView) {
                super(itemView);
                avatar = itemView.findViewById(R.id.account_avatar);
                badge = itemView.findViewById(R.id.account_badge);
                username = itemView.findViewById(R.id.account_username);
                authType = itemView.findViewById(R.id.account_auth_type);
                selectedIndicator = itemView.findViewById(R.id.selected_indicator);
                statusBar = itemView.findViewById(R.id.account_status_bar);
                statusText = itemView.findViewById(R.id.account_status);
                btnRefresh = itemView.findViewById(R.id.btn_refresh);
                btnDelete = itemView.findViewById(R.id.btn_delete);
            }
        }
    }
}

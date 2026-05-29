package dacn.buithikimnhan.cinemabookingapp.user.profile;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dacn.buithikimnhan.cinemabookingapp.R;
import dacn.buithikimnhan.cinemabookingapp.auth.LoginActivity;
import dacn.buithikimnhan.cinemabookingapp.data.User;

public class ProfileFragment extends Fragment {

     ImageView imgAvatar, imgProfileCover, btnProfileManage, btnProfileBack;
    private TextView tvProfileName, tvMemberRank;
    private TextView tvCountWatched, tvCountTickets, tvCountPoints;

    private ViewPager2 vpTickets;
    private LinearLayout layoutEmptyTicket;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private User currentUserModel;

    private Uri selectedImageUri = null;
    private ImageView dialogAvatarRef;
    private ActivityResultLauncher<Intent> galleryLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        initGalleryLauncher();
        initViews(view);
        loadUserDataAndTickets();

        return view;
    }

    private void initViews(View view) {
        imgAvatar = view.findViewById(R.id.imgAvatar);
        imgProfileCover = view.findViewById(R.id.imgProfileCover);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvMemberRank = view.findViewById(R.id.tvMemberRank);
        tvCountWatched = view.findViewById(R.id.tvCountWatched);
        tvCountTickets = view.findViewById(R.id.tvCountTickets);
        tvCountPoints = view.findViewById(R.id.tvCountPoints);
        vpTickets = view.findViewById(R.id.vpTickets);
        layoutEmptyTicket = view.findViewById(R.id.layoutEmptyTicket);

        btnProfileBack = view.findViewById(R.id.btnProfileBack);
        btnProfileManage = view.findViewById(R.id.btnProfileManage);

        if (btnProfileBack != null) {
            btnProfileBack.setOnClickListener(v -> {
                if (getActivity() != null) getActivity().onBackPressed();
            });
        }

        if (btnProfileManage != null) {
            btnProfileManage.setOnClickListener(v -> showEditProfileDialog());
        }

        view.findViewById(R.id.btnLogOut).setOnClickListener(v -> {
            auth.signOut();
            Toast.makeText(getContext(), "Đã đăng xuất tài khoản thành công", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);

            if (getActivity() != null) {
                getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        });
    }

    private void initGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        if (selectedImageUri != null && dialogAvatarRef != null) {
                            Glide.with(this).load(selectedImageUri).into(dialogAvatarRef);
                        }
                    }
                }
        );
    }

    private void showEditProfileDialog() {
        if (currentUserModel == null) {
            Toast.makeText(getContext(), "Đang tải dữ liệu, vui lòng thử lại sau!", Toast.LENGTH_SHORT).show();
            return;
        }

        selectedImageUri = null;

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_edit_profile, null);
        builder.setView(dialogView);

        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false);

        FrameLayout dialogLayoutAvatar = dialogView.findViewById(R.id.dialogLayoutAvatar);
        dialogAvatarRef = dialogView.findViewById(R.id.dialogImgAvatar);
        EditText edtEmail = dialogView.findViewById(R.id.dialogEdtEmail);
        EditText edtFullName = dialogView.findViewById(R.id.dialogEdtFullName);
        EditText edtPhone = dialogView.findViewById(R.id.dialogEdtPhone);
        Button btnCancel = dialogView.findViewById(R.id.dialogBtnCancel);
        Button btnSave = dialogView.findViewById(R.id.dialogBtnSave);

        edtEmail.setText(currentUserModel.getEmail());
        edtFullName.setText(currentUserModel.getFullName());
        edtPhone.setText(currentUserModel.getPhone());

        if (currentUserModel.getAvatar() != null && !currentUserModel.getAvatar().isEmpty() && dialogAvatarRef != null) {
            String avatarData = currentUserModel.getAvatar().trim();

            if (avatarData.startsWith("http")) {
                Glide.with(this).load(avatarData).into(dialogAvatarRef);
            } else {
                try {
                    byte[] decodedString = Base64.decode(avatarData, Base64.DEFAULT);
                    Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                    if (decodedByte != null) {
                        Glide.with(this).load(decodedByte).into(dialogAvatarRef);
                    } else {
                        Glide.with(this).load(android.R.drawable.sym_def_app_icon).into(dialogAvatarRef);
                    }
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                    Glide.with(this).load(android.R.drawable.sym_def_app_icon).into(dialogAvatarRef);
                }
            }
        }

        dialogLayoutAvatar.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            galleryLauncher.launch(intent);
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnSave.setOnClickListener(v -> {
            String newName = edtFullName.getText().toString().trim();
            String newPhone = edtPhone.getText().toString().trim();

            if (newName.isEmpty()) {
                edtFullName.setError("Họ tên không được để trống!");
                return;
            }

            dialog.dismiss();
            updateProfileData(newName, newPhone);
        });

        dialog.show();
    }

    private void updateProfileData(String name, String phone) {
        FirebaseUser firebaseUser = auth.getCurrentUser();
        if (firebaseUser == null) return;

        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Đang lưu thay đổi...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        String uid = firebaseUser.getUid();
        Map<String, Object> updates = new HashMap<>();
        updates.put("fullName", name);
        updates.put("phone", phone);

        if (selectedImageUri != null) {
            String base64Image = uriToBase64(selectedImageUri);
            if (base64Image != null) {
                updates.put("avatar", base64Image);
            }
        } else {
            updates.put("avatar", currentUserModel.getAvatar());
        }

        db.collection("users").document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Cập nhật hồ sơ thành công!", Toast.LENGTH_SHORT).show();
                    loadUserDataAndTickets();
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(getContext(), "Lỗi cập nhật dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String uriToBase64(Uri uri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 60, outputStream);
            byte[] byteArray = outputStream.toByteArray();

            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void loadUserDataAndTickets() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) return;

        String uid = currentUser.getUid();

        db.collection("users").document(uid).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists() && isAdded()) {
                        currentUserModel = documentSnapshot.toObject(User.class);

                        if (currentUserModel != null) {
                            if (currentUserModel.getUid() == null || currentUserModel.getUid().isEmpty()) {
                                currentUserModel.setUid(documentSnapshot.getId());
                            }

                            tvProfileName.setText(currentUserModel.getFullName() != null ? currentUserModel.getFullName() : "Người dùng");

                            Long points = documentSnapshot.getLong("points");
                            String rank = documentSnapshot.getString("rank");
                            tvCountPoints.setText(String.valueOf(points != null ? points : 0));

                            if (rank != null) {
                                tvMemberRank.setText("THÀNH VIÊN " + rank.toUpperCase());
                            } else {
                                tvMemberRank.setText("THÀNH VIÊN CHUẨN");
                            }

                            if (currentUserModel.getAvatar() != null && !currentUserModel.getAvatar().isEmpty() && getView() != null) {
                                String avatarData = currentUserModel.getAvatar().trim();

                                if (avatarData.startsWith("http")) {
                                    if (imgAvatar != null) Glide.with(this).load(avatarData).into(imgAvatar);
                                    if (imgProfileCover != null) Glide.with(this).load(avatarData).into(imgProfileCover);
                                } else {
                                    try {
                                        byte[] decodedString = Base64.decode(avatarData, Base64.DEFAULT);
                                        Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);

                                        if (decodedByte != null) {
                                            if (imgAvatar != null) Glide.with(this).load(decodedByte).into(imgAvatar);
                                            if (imgProfileCover != null) Glide.with(this).load(decodedByte).into(imgProfileCover);
                                        } else {
                                            if (imgAvatar != null) Glide.with(this).load(android.R.drawable.sym_def_app_icon).into(imgAvatar);
                                            if (imgProfileCover != null) Glide.with(this).load(android.R.drawable.sym_def_app_icon).into(imgProfileCover);
                                        }
                                    } catch (IllegalArgumentException e) {
                                        e.printStackTrace();
                                        if (imgAvatar != null) Glide.with(this).load(android.R.drawable.sym_def_app_icon).into(imgAvatar);
                                        if (imgProfileCover != null) Glide.with(this).load(android.R.drawable.sym_def_app_icon).into(imgProfileCover);
                                    }
                                }
                            }
                        }
                    }
                });

        db.collection("bookings")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "checked_in")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;
                    if (queryDocumentSnapshots != null) {
                        int watchedCount = queryDocumentSnapshots.size();
                        tvCountWatched.setText(String.valueOf(watchedCount));
                    } else {
                        tvCountWatched.setText("0");
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) tvCountWatched.setText("0");
                });

        db.collection("bookings")
                .whereEqualTo("userId", uid)
                .whereEqualTo("status", "booked")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (!isAdded()) return;

                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {

                        List<DocumentSnapshot> validBookedTickets = new ArrayList<>();

                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            String currentStatus = doc.getString("status");

                            // Bảo mật logic: Nếu có bất kỳ vé nào khác trạng thái booked hoặc bị null thì không cho hiện
                            if (currentStatus == null || !currentStatus.equalsIgnoreCase("booked")) {
                                continue;
                            }
                            validBookedTickets.add(doc);
                        }

                        // Kiểm tra lại danh sách vé booked sau khi lọc
                        if (!validBookedTickets.isEmpty()) {
                            int totalTickets = validBookedTickets.size();
                            tvCountTickets.setText(String.valueOf(totalTickets)); // Cập nhật đúng số lượng vé chưa đi xem

                            TicketPagerAdapter pagerAdapter = new TicketPagerAdapter(validBookedTickets);
                            vpTickets.setAdapter(pagerAdapter);
                            vpTickets.setOffscreenPageLimit(1);

                            vpTickets.setVisibility(View.VISIBLE);
                            layoutEmptyTicket.setVisibility(View.GONE);
                        } else {
                            handleEmptyTickets();
                        }
                    } else {
                        handleEmptyTickets();
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        handleEmptyTickets();
                    }
                });
    }

    private void handleEmptyTickets() {
        vpTickets.setVisibility(View.GONE);
        layoutEmptyTicket.setVisibility(View.VISIBLE);
        tvCountTickets.setText("0");
    }
}
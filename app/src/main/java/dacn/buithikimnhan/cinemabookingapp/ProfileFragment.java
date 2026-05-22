package dacn.buithikimnhan.cinemabookingapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class ProfileFragment extends Fragment {

    TextView tvProfileName;

    FirebaseAuth mAuth;

    public ProfileFragment() {

    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {

        View view = inflater.inflate(
                R.layout.fragment_profile,
                container,
                false
        );

        mAuth = FirebaseAuth.getInstance();

        tvProfileName = view.findViewById(R.id.tvProfileName);

        loadUserInfo();

        return view;
    }

    private void loadUserInfo() {

        FirebaseUser currentUser =
                mAuth.getCurrentUser();

        if (currentUser != null) {

            String name =
                    currentUser.getDisplayName();

            String email =
                    currentUser.getEmail();

            if (name != null &&
                    !name.isEmpty()) {

                tvProfileName.setText(name);

            } else {

                if (email != null &&
                        email.contains("@")) {

                    String tempName =
                            email.split("@")[0];

                    tvProfileName.setText(
                            tempName
                    );
                }
            }

        } else {

            tvProfileName.setText(
                    "Khách hàng mới"
            );
        }
    }
}
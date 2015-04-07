package ch.bfh.anuto;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import ch.bfh.anuto.game.GameManager;

public class StatusFragment extends Fragment implements GameManager.CreditsListener,
        GameManager.LivesListener, GameManager.WaveListener {

    private GameManager mManager;

    private TextView txt_credits;
    private TextView txt_lives;
    private TextView txt_wave;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_status, container, false);

        txt_credits = (TextView) v.findViewById(R.id.txt_credits);
        txt_lives = (TextView) v.findViewById(R.id.txt_lives);
        txt_wave = (TextView) v.findViewById(R.id.txt_wave);

        return v;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        mManager = ((MainActivity) activity).getGame().getManager();
        mManager.addListener(this);
    }

    @Override
    public void onDetach() {
        super.onDetach();

        mManager.removeListener(this);
    }


    @Override
    public void onNextWave() {
        txt_wave.post(new Runnable() {
            @Override
            public void run() {
                txt_wave.setText("Wave: " + mManager.getWaveNum());
            }
        });

    }

    @Override
    public void onWaveDone() {

    }

    @Override
    public void onCreditsChanged() {
        txt_credits.post(new Runnable() {
            @Override
            public void run() {
                txt_credits.setText("Credits: " + mManager.getCredits());
            }
        });
    }

    @Override
    public void onLivesChanged() {
        txt_lives.post(new Runnable() {
            @Override
            public void run() {
                txt_lives.setText("Lives: " + mManager.getLives());
            }
        });
    }
}
package com.example.petping;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.squareup.picasso.Picasso;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import org.tensorflow.lite.Interpreter;

public class HomeFragment extends Fragment {
    private ViewFlipper flipper;
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ArrayList<PetSearch> petList = new ArrayList<>();
      private ArrayList<Content> contentList = new ArrayList<>();
    private HomeAdapter homeAdapter;
    private ContentHomeAdapter contentAdapter;
    private RecyclerView pet_rec, contentView;
    private TextView seePet;
    private String modelFile="converted_model_test.tflite";
    private Interpreter tflite;
    private int count;

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, @Nullable ViewGroup container, @Nullable final Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_home, null);
        int image[]= {R.drawable.flip1, R.drawable.flip2, R.drawable.flip3};
        flipper = view.findViewById(R.id.flipper_home);
        seePet = view.findViewById(R.id.see_pet);
        try {
            tflite = new Interpreter(loadModelFile(modelFile));
        }  catch (IOException e) {
            e.printStackTrace();
        }
        final float[][][][] inp = new float[1][224][224][3];
        final float[][] out = new float[1][1000];

//        Random rand = new Random();
//        for(int i=0; i<1; i++){
//            for(int j=0; j<10; j++){
//                for(int k=0; k<10; k++){
//                    for(int l=0; l<3; l++){
//                        inp[i][j][k][l] = rand.nextInt(50);
////                        Log.d("Input", String.valueOf(inp[i][j][k][l]));
//                    }
//                }
//            }
//        }
//        Log.d("Inp", String.valueOf(inp));

//        for(int i=0; i<1 ;i++){
//            for(int j=0; j<1000; j++){
//                out[i][j] = rand.nextInt(50);
////                Log.d("Output", String.valueOf(out[i][j]));
//            }
//        }


//        for(int i=0; i<1 ;i++){
//            for(int j=0; j<1000; j++){
//                Log.d("OutputReal", String.valueOf(out[i][j]));
//            }
//        }


        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        pet_rec = view.findViewById(R.id.pet_rec);
        pet_rec.setLayoutManager(layoutManager);

        LinearLayoutManager layoutManager1 = new LinearLayoutManager(getContext(), LinearLayoutManager.HORIZONTAL, false);
        contentView = view.findViewById(R.id.content);
        contentView.setLayoutManager(layoutManager1);

        for(int i=0; i<image.length; i++){
            flipperImages(image[i]);
        }

        if(!petList.isEmpty()){
            petList.clear();
        }
        if(!contentList.isEmpty()){
            contentList.clear();
        }

        db.collection("Content")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            for (QueryDocumentSnapshot document : task.getResult()) {
                                Content content = new Content(document.getId(), document.get("Topic").toString(),
                                        document.get("Story").toString(), document.get("URL").toString(), document.get("ShelterID").toString(),
                                        document.get("Date").toString(), document.get("Time").toString());
                                contentList.add(content);
//                                Log.d("Content",document.get("Topic").toString());
                            }
                            contentAdapter = new ContentHomeAdapter(getFragmentManager(), getId(), getContext(), contentList);
                            contentView.setAdapter(contentAdapter);
                        }
                    }
                });


//        db.collection("Pet")
//                .whereEqualTo("Status", "กำลังหาบ้าน")
//                .get()
//                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
//                    @Override
//                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
//                        if (task.isSuccessful()) {
//                            for (QueryDocumentSnapshot document : task.getResult()) {
//                                PetSearch petSearch = new PetSearch(document.getId(), document.get("Name").toString(), document.get("Type").toString(),
//                                        document.get("Color").toString(), document.get("Sex").toString(), document.get("Age").toString(),
//                                        document.get("Breed").toString(), document.get("Size").toString(), document.get("Image").toString(),
//                                        document.get("Weight").toString(), document.get("Character").toString(), document.get("Marking").toString(),
//                                        document.get("Health").toString(), document.get("OriginalLocation").toString(), document.get("Status").toString(),
//                                        document.get("Story").toString(), document.get("ShelterID").toString());
//                                petList.add(petSearch);
//                            }
//                            homeAdapter = new HomeAdapter(getFragmentManager(), getId(), getContext(), petList);
//                            pet_rec.setAdapter(homeAdapter);
//                        }
//                    }
//                });


        db.collection("User")
                .document(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .collection("Like")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if(task.isSuccessful()){
                            ArrayList<Float> inputList = new ArrayList<>();

                            for(final QueryDocumentSnapshot document : task.getResult()){
                                inputList.add(Float.valueOf(document.get("Rec").toString()));
                                for(int i=0; i<1; i++){
                                    for(int j=0; j<5; j++){
                                        for(int k=0; k<5; k++){
                                            for(int l=0; l<3; l++){
                                                inp[i][j][k][l] = Float.valueOf(document.get("Rec").toString());

                                            }
                                        }
                                    }
                                }
                                tflite.run(inp, out);
                                recommed(out);
                            }

                        }
                    }
                });

        seePet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                RecommendPetFragment pet = new RecommendPetFragment();
                Bundle bundle = new Bundle();
                bundle.putParcelableArrayList("recommendPet", petList);
                pet.setArguments(bundle);
                FragmentTransaction ft = getFragmentManager().beginTransaction();
                ft.replace(getId(), pet);
                ft.addToBackStack(null).commit();
            }
        });

        return view;
    }

    private void recommed(final float[][] out) {
                db.collection("Pet")
                .whereEqualTo("Status", "กำลังหาบ้าน")
                .get()
                .addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<QuerySnapshot> task) {
                        if (task.isSuccessful()) {
                            int[] output = new int[1000];
                            for(int i=0; i<1 ;i++){
                                for(int j=0; j<1000; j++){
                                    String num = String.valueOf(out[i][j]);
                                    String[] parts = num.split(Pattern.quote("."));
                                    String part = parts[0]; // 004
                                    output[j] = Integer.parseInt(part);
                                    Log.d("Num", String.valueOf(output[j] ));
                                }
                            }


                            Map<Integer,Integer> countMap = new HashMap<>();
                            for(int i=0;i<output.length;i++){
                                if(countMap.containsKey(output[i])){
                                    countMap.put(output[i], countMap.get(output[i])+1 );
                                }else{
                                    countMap.put(output[i], 1);
                                }
                            }

                            String key = null;
                            String value=null;
                            for (Integer in: countMap.keySet()){
                                key = in.toString();
                                value = countMap.get(in).toString();
                                Log.d("key", key);
                                Log.d("value", value);
                                for (QueryDocumentSnapshot document : task.getResult()) {
                                    if(document.get("Rec").toString().equals(key)){
                                        PetSearch petSearch = new PetSearch(document.getId(), document.get("Name").toString(), document.get("Type").toString(),
                                        document.get("Color").toString(), document.get("Sex").toString(), document.get("Age").toString(),
                                        document.get("Breed").toString(), document.get("Size").toString(), document.get("Image").toString(),
                                        document.get("Weight").toString(), document.get("Character").toString(), document.get("Marking").toString(),
                                        document.get("Health").toString(), document.get("OriginalLocation").toString(), document.get("Status").toString(),
                                        document.get("Story").toString(), document.get("ShelterID").toString());
                                        petList.add(petSearch);
                                        Log.d("PetName", document.get("Name").toString());
                                    }
                                }
                            }

                            homeAdapter = new HomeAdapter(getFragmentManager(), getId(), getContext(), petList);
                            pet_rec.setAdapter(homeAdapter);
                        }
                    }
                });

    }


    public void flipperImages(int image){
        ImageView img = new ImageView(getContext());
        img.setBackgroundResource(image);
        flipper.addView(img);
        flipper.setFlipInterval(4000);
        flipper.setAutoStart(true);
    }

    private MappedByteBuffer loadModelFile(String modelFile) throws IOException {
        AssetFileDescriptor assetFileDescriptor = getContext().getAssets().openFd(modelFile) ;
        FileInputStream fileInputStream = new FileInputStream( assetFileDescriptor.getFileDescriptor() ) ;
        FileChannel fileChannel = fileInputStream.getChannel() ;
        long startoffset = assetFileDescriptor.getStartOffset() ;
        long declaredLength = assetFileDescriptor.getDeclaredLength() ;
        return fileChannel.map( FileChannel.MapMode.READ_ONLY , startoffset , declaredLength) ;
    }
}

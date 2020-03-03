package com.example.davea.mapping_gps;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;



public class ViewData extends AppCompatActivity {

    FileInputStream inStream;   //input stream from the file
    BufferedReader reader;  //reader to make the data useful

    //UI:
    TextView TV2;
    Button btnDelete;
    Button btnExport;
    Button btnEmail;

    String fileContents;
    boolean fileExists = true;

    Toast myToast = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_data);


        setup();

        readFile();

        btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(fileExists) {   //check to see if there is a file before trying to delete it
                    getDeleteConfirmation();    //only delete after confirming
                }
                else {
                    if(myToast != null) myToast.cancel();
                    myToast = Toast.makeText(getApplicationContext(), "ERROR: File not found", Toast.LENGTH_SHORT);
                    myToast.show();
                }
            }
        });

        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(fileExists) {//check to see if there is a file
                    ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);    //instantiate clipboard manager
                    ClipData clip = ClipData.newPlainText("clip", TV2.getText());  //copy data from textview into clipboard
                    if (clipboard != null) {    //ensure clipboard exists
                        clipboard.setPrimaryClip(clip); //set as a clip
                    }
                    else{   //warn if clipboard does not exist
                        if(myToast != null) myToast.cancel();
                        myToast = Toast.makeText(getApplicationContext(), "ERROR: Clipboard not available", Toast.LENGTH_SHORT);
                        myToast.show();

                    }
                    if(myToast != null) myToast.cancel();
                    myToast = Toast.makeText(getApplicationContext(), "Copied to Clipboard", Toast.LENGTH_SHORT);
                    myToast.show();
                }
                else {
                    if(myToast != null) myToast.cancel();
                    myToast = Toast.makeText(getApplicationContext(), "ERROR: File not found", Toast.LENGTH_SHORT);
                    myToast.show();
                }
            }
        });

        btnEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {//check to see if there is a file
                if(fileExists) email();
                else{
                    if(myToast != null) myToast.cancel();
                    myToast = Toast.makeText(getApplicationContext(), "ERROR: File not found", Toast.LENGTH_SHORT);
                    myToast.show();
                }
            }
        });

    }

    void setup(){
        //define UI stuff
        TV2 = findViewById(R.id.TV2);
        TV2.setMovementMethod(new ScrollingMovementMethod());
        btnDelete = findViewById(R.id.btnDelete);
        btnExport = findViewById(R.id.export);
        btnEmail = findViewById(R.id.btnEmail);

    }

    void readFile(){
        try {
            // try to open and read the file, but catch exceptions
            inStream = openFileInput(MapsActivity.filename); //open file and set as input stream
            reader = new BufferedReader(new InputStreamReader(new DataInputStream(inStream)));  //set value of reader
            String line;    //declare string to read in one line at a time
            while((line = reader.readLine()) != null){
                TV2.setText(TV2.getText() + line + "\n");   //set textview to output the line
                fileContents += line;
            }//keep outputting lines until end of file is reached
            inStream.close();   //close file once finished reading through the file
            fileExists = true; //there is a file
        } catch (FileNotFoundException e) { //catch exceptions
            e.printStackTrace();
            TV2.setText("");
            if(myToast != null) myToast.cancel();
            myToast = Toast.makeText(getApplicationContext(), "File not found", Toast.LENGTH_SHORT);
            myToast.show();
            fileExists = false;  //there is no file
        } catch (IOException e) {
            e.printStackTrace();
            if(myToast != null) myToast.cancel();
            myToast = Toast.makeText(getApplicationContext(), "error reading file\ncannot read in lines", Toast.LENGTH_SHORT);
            myToast.show();
        }
    }

    void email(){
        /*File filelocation = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), MapsActivity.filename);
        Uri path = Uri.fromFile(filelocation);*/
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        // set the type to 'email'
        emailIntent.setType("vnd.android.cursor.dir/email");
        String to[] = {""};
        emailIntent.putExtra(Intent.EXTRA_EMAIL, to);
        // the attachment - does nto work
        //emailIntent.putExtra(Intent.EXTRA_STREAM, path);
        // the mail subject
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Mapping Data");
        //send data in body of email becasue attachment reads as empty
        emailIntent.putExtra(Intent.EXTRA_TEXT, TV2.getText());

        startActivity(Intent.createChooser(emailIntent , "Send email"));
    }

    void getDeleteConfirmation(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);  //instantiate alert box
        alert.setMessage("Are you sure you want to delete the file?");  //ask question
        alert.setCancelable(false); //user must respond with option on the dialog box

        alert.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                delete();   //if they click Yes, delete the file
            }
        });

        alert.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //if no, do nothing
            }
        });

        alert.create().show();  //create and display the dialog box


    }

    void delete(){
        deleteFile(MapsActivity.filename);   //delete the file
        TV2.setText("");    //clear TV2
        MapsActivity.fileContents = null;   //clear contents of fileContents so that it is not rewritten next time new data is added to the file
        if(myToast != null) myToast.cancel();
        myToast = Toast.makeText(getApplicationContext(), "File Deleted", Toast.LENGTH_SHORT);
        myToast.show();
        fileExists = false; //file is now gone
    }

}

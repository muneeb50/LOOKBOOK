package com.htsm.lookbook.Fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.htsm.lookbook.Controllers.BooksController;
import com.htsm.lookbook.Models.Book;
import com.htsm.lookbook.R;

/**
 * Created by saboor on 12/28/2017.
 */

public class AddBookFragment extends Fragment
{
    private static final String TAG = "AddBookFragment";
    private static final String KEY_BOOK_OBJ = "AddBookFragment.editObj";
    private static final String KEY_BOOK_ID = "AddBookFragment.editId";

    private EditText mBookNameInput;
    private EditText mBookAuthorInput;
    private EditText mBookEditionInput;

    private Button mAddBookButton;

    private BooksController mBooksController;
    private String mBookId;
    private Book mBook;
    private String mSnackBarText;

    private AlertDialog mAlertDialog;

    public static AddBookFragment newEditInstance(Book book, String id) {
        Bundle args = new Bundle();
        args.putSerializable(KEY_BOOK_OBJ, book);
        args.putString(KEY_BOOK_ID, id);
        AddBookFragment fragment = new AddBookFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static AddBookFragment newAddInstance() {
        return new AddBookFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBooksController = new BooksController();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_add_book, container, false);

        if(getArguments() != null)
            mBookId = getArguments().getString(KEY_BOOK_ID);

        mBookNameInput = v.findViewById(R.id.id_book_name);
        mBookAuthorInput = v.findViewById(R.id.id_book_author);
        mBookEditionInput = v.findViewById(R.id.id_book_edition);
        mAddBookButton = v.findViewById(R.id.id_btn_add_book);

        mAlertDialog = new AlertDialog.Builder(getActivity())
                .setTitle("Adding Book...")
                .setView(new ProgressBar(getActivity()))
                .create();

        if(mBookId != null) {
            mBook = (Book) getArguments().getSerializable(KEY_BOOK_OBJ);
            getActivity().setTitle("Edit Book Info");
            mAddBookButton.setText("Update");
        }

        mAlertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialogInterface) {
                Snackbar.make(AddBookFragment.this.getView(), mSnackBarText, Snackbar.LENGTH_LONG).show();
            }
        });

        mAddBookButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                InputMethodManager inputManager = (InputMethodManager)
                        getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);

                inputManager.hideSoftInputFromWindow(getActivity().getCurrentFocus().getWindowToken(),
                        InputMethodManager.HIDE_NOT_ALWAYS);

                if(mBookNameInput.getText().length() > 0 && mBookAuthorInput.getText().length() > 0 && mBookEditionInput.getText().length() > 0) {
                    mAlertDialog.show();
                    mBooksController.addUpdateBook(mBookNameInput.getText().toString(), mBookAuthorInput.getText().toString(), Integer.parseInt(mBookEditionInput.getText().toString()), mBookId, new BooksController.OnTaskCompletedListener() {
                        @Override
                        public void onTaskSuccessful() {
                            mSnackBarText = mBookId == null ? "Book has been added" : "Book Info updated!";
                            mAlertDialog.dismiss();
                            if(mBookId == null) {
                                mBookNameInput.setText("");
                                mBookAuthorInput.setText("");
                                mBookEditionInput.setText("");
                            }
                        }

                        @Override
                        public void onTaskFailed(Exception ex) {
                            mSnackBarText = "Error Occurred!";
                            mAlertDialog.dismiss();
                            Log.wtf(TAG, ex.toString());
                        }
                    });
                }
                else {
                    mSnackBarText = "Fill out all the fields!";
                }
            }
        });

        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if(mBook != null) {
            mBookNameInput.setText(mBook.getBookName());
            mBookAuthorInput.setText(mBook.getBookAuthor());
            mBookEditionInput.setText(mBook.getBookEdition() + "");
        }
    }
}

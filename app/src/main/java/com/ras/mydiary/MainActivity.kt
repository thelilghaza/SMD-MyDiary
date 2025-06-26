package com.ras.mydiary

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import com.google.android.material.navigation.NavigationView
import com.ras.mydiary.databinding.ActivityMainBinding
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        binding.appBarMain.fab.setOnClickListener {
            val navController = findNavController(R.id.nav_host_fragment_content_main)
            navController.navigate(R.id.nav_manual_journal)
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main) as NavHostFragment
        val navController = navHostFragment.navController

        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.nav_home, R.id.nav_manual_journal, R.id.nav_ai_journal, R.id.nav_therapists, R.id.nav_settings)
        )

        setupActionBarWithNavController(navController, appBarConfiguration)

        val menuButton = findViewById<ImageButton>(R.id.hamburger_icon)
        menuButton.setOnClickListener {
            if (!drawerLayout.isDrawerOpen(GravityCompat.END)) {
                drawerLayout.openDrawer(GravityCompat.END)
            } else {
                drawerLayout.closeDrawer(GravityCompat.END)
            }
        }

        navView.setNavigationItemSelectedListener { menuItem ->
            val handled = when (menuItem.itemId) {
                R.id.nav_home -> {
                    navController.navigate(R.id.nav_home)
                    true
                }
                R.id.nav_ai_journal -> {
                    navController.navigate(R.id.nav_ai_journal)
                    true
                }
                R.id.nav_manual_journal -> {
                    navController.navigate(R.id.nav_manual_journal)
                    true
                }
                R.id.nav_therapists -> {
                    navController.navigate(R.id.nav_therapists)
                    true
                }
                R.id.nav_settings -> {
                    navController.navigate(R.id.nav_settings)
                    true
                }
                R.id.nav_logout -> {
                    FirebaseAuth.getInstance().signOut() // Sign out from Firebase
                    Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, LoginActivity::class.java)
                    startActivity(intent)
                    finish()
                    true
                }
                else -> false
            }

            if (handled) {
                drawerLayout.closeDrawer(GravityCompat.END)
            }

            handled
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.nav_manual_journal || destination.id == R.id.nav_ai_journal || destination.id == R.id.nav_therapists || destination.id == R.id.nav_settings) {
                binding.appBarMain.fab.hide()
            } else {
                binding.appBarMain.fab.show()
            }
        }

        val headerView = navView.getHeaderView(0)
        val imageView = headerView.findViewById<ImageView>(R.id.imageView_profile)
        val usernameText = headerView.findViewById<TextView>(R.id.textView_username)
        val emailText = headerView.findViewById<TextView>(R.id.textView_email)

        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val database = FirebaseDatabase.getInstance()
            val uid = user.uid

            val userRef = database.getReference("Users").child(uid)
            userRef.get().addOnSuccessListener { dataSnapshot ->
                val name = dataSnapshot.child("name").getValue(String::class.java)
                val profileImageUrl = dataSnapshot.child("profileImageUrl").getValue(String::class.java)

                // Set username and email
                usernameText.text = name ?: "N/A"
                emailText.text = user.email ?: "N/A"

                // Load the profile image using Glide (or Base64 if needed)
                if (!profileImageUrl.isNullOrEmpty()) {
                    try {
                        // Assuming profileImageUrl is Base64
                        val decodedBytes = Base64.decode(profileImageUrl, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                        imageView.setImageBitmap(bitmap)
                    } catch (e: Exception) {
                        // Fallback to a default image if decoding fails
                        Glide.with(this)
                            .load(profileImageUrl)
                            .placeholder(R.drawable.icon_profile_foreground)
                            .circleCrop()
                            .into(imageView)
                    }
                } else {
                    // Use default profile image if no URL is available
                    Glide.with(this)
                        .load(R.drawable.icon_profile_foreground)
                        .circleCrop()
                        .into(imageView)
                }
            }.addOnFailureListener {
                // Handle failure
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}
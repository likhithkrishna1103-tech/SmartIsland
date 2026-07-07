# Smart Island Proguard Rules

# Keep Compose Runtime and library classes
-keep class androidx.compose.** { *; }

# Keep DataStore Preferences serializer/keys
-keep class androidx.datastore.preferences.core.** { *; }

# Keep reflection targets used in SmartIslandOverlayService
-keep class android.view.ViewTreeObserver$OnComputeInternalInsetsListener { *; }
-keep class android.view.ViewTreeObserver {
    public void addOnComputeInternalInsetsListener(android.view.ViewTreeObserver$OnComputeInternalInsetsListener);
}
-keep class android.view.View$InternalInsetsInfo {
    public void setTouchableInsets(int);
    *** mTouchableRegion;
    *** touchableRegion;
}

# Keep ActivityOptions and setLaunchWindowingMode
-keep class android.app.ActivityOptions {
    public void setLaunchWindowingMode(int);
    public void setPendingIntentBackgroundActivityStartMode(int);
}

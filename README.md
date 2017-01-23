[![Build Status](https://travis-ci.org/ncapdevi/FragNav.svg?branch=master)](https://travis-ci.org/ncapdevi/FragNav)

# FragNav
Android library for managing multiple stacks of fragments (e.g., [Bottom Navigation ](https://www.google.com/design/spec/components/bottom-navigation.html), [Navigation Drawer](https://www.google.com/design/spec/patterns/navigation-drawer.html)).  This library does NOT include the UI for bottom tab bar layout.  For that, I recommend either [BottomBar](https://github.com/roughike/BottomBar) (which is the library shown in the demo) or [AHBottomNavigation](https://github.com/aurelhubert/ahbottomnavigation). This library helps maintain order after pushing onto and popping from multiple stacks(tabs). It also helps with switching between desired tabs and clearing the stacks.

<img src="https://raw.githubusercontent.com/ncapdevi/FragNav/master/FragNavDemo.gif" width="30%" />

## Sample
With [Material Design Bottom Navigation pattern](https://www.google.com/design/spec/components/bottom-navigation.html), and other tabbed navigation, managing multiple stacks of fragments can be a real headache.  The example file shows best practice for navigating deep within a tab stack.

## Gradle

```groovy
compile 'com.ncapdevi:frag-nav:1.3.0'
```

## How do I implement it?

### Initialize one of two ways
#### 1.
Create a list of fragments and pass them in
```java
List<Fragment> fragments = new ArrayList<>(5);

fragments.add(RecentsFragment.newInstance());
fragments.add(FavoritesFragment.newInstance());
fragments.add(NearbyFragment.newInstance());
fragments.add(FriendsFragment.newInstance());
fragments.add(FoodFragment.newInstance());

FragNavController fragNavController =
                new FragNavController(savedInstanceState, getSupportFragmentManager(), R.id.container, fragments, INDEX_RECENTS);
```
#### 2.

Allow for dynamically creating the base class by implementing the NavListener in your class and overriding the getRootFragment method

```java
public class YourActivity extends AppCompatActivity implements FragNavController.RootFragmentListener {
```

```java
        mNavController =
                new FragNavController(savedInstanceState, getSupportFragmentManager(), R.id.container,this,5, INDEX_NEARBY);

```
```java

    @Override
    public Fragment getRootFragment(int index) {
        switch (index) {
            case INDEX_RECENTS:
                return RecentsFragment.newInstance(0);
            case INDEX_FAVORITES:
                return FavoritesFragment.newInstance(0);
            case INDEX_NEARBY:
                return NearbyFragment.newInstance(0);
            case INDEX_FRIENDS:
                return FriendsFragment.newInstance(0);
            case INDEX_FOOD:
                return FoodFragment.newInstance(0);
        }
        throw new IllegalStateException("Need to send an index that we know");
    }
```


Send in  the supportFragment Manager, a list of base fragments, the container that you'll be using to display fragments.
After that, you have four main functions that you can use
In your activity, you'll also want to override your onSaveInstanceState like so

```
   @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mNavController != null) {
            mNavController.onSaveInstanceState(outState);
        }
    }
```

### Switch tabs
Tab switching is indexed to try to prevent you from sending in wrong indices. It also will throw an error if you try to switch to a tab you haven't defined a base fragment for.

```java
fragNavController.switchTab(NavController.TAB1);
fragNavController.switchTab(NavController.TAB2);
fragNavController.switchTab(NavController.TAB3);
fragNavController.switchTab(NavController.TAB4);
fragNavController.switchTab(NavController.TAB5);
```

### Push a fragment
You can only push onto the currently selected index

        fragNavController.pushFragment(FoodFragment.newInstance())

### Pop a fragment
You can only pop from the currently selected index. This can throw an UnsupportedOperationException if trying to pop the root fragment

        fragNavController.popFragment();

### Pop multiple fragments
You can pop multiple fragments at once, with the same rules as above applying.  If the pop depth is deeper than possible, it will stop when it gets to the root fragment

       fragNavController.popFragments(3);

### Replacing a fragment
You can only replace onto the currently selected index

        fragNavController.replaceFragment(Fragment fragment);

### You can also clear the stack to bring you back to the base fragment
        fragNavController.clearStack();

### You can also navigate your DialogFragments using
        showDialogFragment(dialogFragment);
        clearDialogFragment();
        getCurrentDialogFrag()

### Get informed of fragment transactions
Have your activity implement FragNavController.TransactionListener
 and you will have methods that inform you of tab switches or fragment transactions

A sample application is in the repo if you need to see how it works.

### Fragment Transitions

Use FragNavController.setTransitionMode();

### Helper functions

    /**
     * Get the number of fragment stacks
     * @return the number of fragment stacks
     */
    public int getSize()

    /**
     * Get the current stack that is being displayed
     * @return Current stack
     */
    public Stack<Fragment> getCurrentStack()
    
    /**
     * @return  If true, you are at the bottom of the stack
     * (Consider using replace if you need to change the root fragment for some reason)
     * else you can pop as needed as your are not at the root
     *  * @deprecated use {@link #isRootFragment()} instead.
     */
    public boolean isRootFragment() {

    /**
     *
     * @return Current DialogFragment being displayed. Null if none
     */
    @Nullable
    public DialogFragment getCurrentDialogFrag()

    /**
     * Clear any DialogFragments that may be shown
     */
    public void clearDialogFragment()

    /**
     *  Display a DialogFragment on the screen
     * @param dialogFragment The Fragment to be Displayed
     */
    public void showDialogFragment(DialogFragment dialogFragment)

## Apps Using FragNav
Feel free to send me a pull request with your app and I'll link you here:

## Contributions
If you have any problems, feel free to create an issue or pull request.

The sample app in the repository uses [BottomBar](https://github.com/roughike/BottomBar) library.

## License

```
FragNav Android Fragment Navigation Library
Copyright (c) 2016 Nic Capdevila (http://github.com/ncapdevi).

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

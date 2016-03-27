# FragNav
Android library for managing multiple stacks of fragments.

<img src="https://raw.githubusercontent.com/ncapdevi/FragNav/master/FragNavDemo.mp4" width="30%" />

## Use
With [Material Design Bottom Navigation pattern](https://www.google.com/design/spec/components/bottom-navigation.html), and other tabbed navigation, managing multiple stacks of fragments can be a real headache. This library helps maintain pushing and popping onto, as well as switching between, desired tabs.  

## Gradle
    compile 'com.ncapdevi:frag-nav:1.0.1'

## How do I implement it?

### Initialize
        List<Fragment> fragments = new ArrayList<>(5);

        fragments.add(RecentsFragment.newInstance());
        fragments.add(FavoritesFragment.newInstance());
        fragments.add(NearbyFragment.newInstance());
        fragments.add(FriendsFragment.newInstance());
        fragments.add(FoodFragment.newInstance());

        FragNavController fragNavController = new FragNavController(getSupportFragmentManager(),R.id.container,fragments);

Send in  the supportFragment Manager, a list of base fragments, the container that you'll be using to display fragments.
After that, you have four main functions that you can use

### Switch tabs
Tab switching is indexed to try to prevent you from sending in wrong indices. It also will throw an error if you try to switch to a tab you haven't defined a base fragment for.

        fragNavController.switchTab(NavController.TAB1);
        fragNavController.switchTab(NavController.TAB2);
        fragNavController.switchTab(NavController.TAB3);
        fragNavController.switchTab(NavController.TAB4);
        fragNavController.switchTab(NavController.TAB5);
        
### Push a fragment
You can only push onto the currently selected index
        fragNavController.push(FoodFragment.newInstance())
        
### Popping a fragment only happens on the same index as well
        fragNavController.pop();
        
### You can also clear the stack to bring you back to the base fragment
    fragNavController.clearStack();
    
    
A sample application is in the repo if you need to see how it works.


## Notes

The sample app uses (as well do I recommend) the great BottomBar library to keep up with the material design spec sheet. https://github.com/roughike/BottomBar

## License

```
FragNav Android fragment Library
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
    


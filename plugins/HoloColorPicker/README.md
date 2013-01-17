<h1>Android Holo ColorPicker</h1>

A Holo themed colorpicker designed by Marie Schweiz and Nick Butcher.

![image](https://lh3.googleusercontent.com/-RzpEyLl-1xM/UOMyztql1gI/AAAAAAAAATs/UKBuqZZtaZw//HoloColorPickerFramed1.png)
![image](https://lh4.googleusercontent.com/-sXAPd8onJ_8/UOMyzjA6c4I/AAAAAAAAATo/DY4kIzo7TtU//HoloColorPickerFramed2.png)


<h2>Documentation</h2>
Add this to your xml

	<com.larswerkman.colorpicker.ColorPicker
        android:id="@+id/picker"
                android:layout_width="285dp"
        android:layout_height="290dp"/>
        
To change the thickness of the wheel and the pointer you can add this.
 
 	app:wheel_size="2"
    app:pointer_size="4"

To get the color of the colorpicker

	ColorPicker picker = (ColorPicker) findViewById(R.id.picker);
	
	picker.getColor();
	
<H2>License</H2>
	
 	 Copyright 2012 Lars Werkman
 	
 	 Licensed under the Apache License, Version 2.0 (the 	"License");
 	 you may not use this file except in compliance with 	the License.
 	 You may obtain a copy of the License at
 	
 	     http://www.apache.org/licenses/LICENSE-2.0
 	
 	 Unless required by applicable law or agreed to in 	writing, software
	 distributed under the License is distributed on an 	"AS IS" BASIS,
 	 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, 	either express or implied.
 	 See the License for the specific language governing 	permissions and
 	 limitations under the License.
 	

<h2>Devoleped By</h2>
**Lars Werkman**

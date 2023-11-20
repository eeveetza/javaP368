# Java Implementation of Recommendation ITU-R P.368

This code repository contains a Java software implementation of  [Recommendation ITU-R P.368-10](https://www.itu.int/rec/R-REC-P.368/en)  with a ground-wave propagation prediction method for frequencies between 10 kHz and 30 MHz.  

This version of the code is functionally identical to the reference version approved by ITU-R Working Party 3L and published by Study Group 3 on [ITU-R SG 3 Software, Data, and Validation Web Page](https://www.itu.int/en/ITU-R/study-groups/rsg3/Pages/iono-tropo-spheric.aspx). 
<!--This version of the code is also implemented in [SEAMCAT](https://seamcat.org). -->

This is a translation of the original reference C++ implementation of this Recommendation available at [NTIA/LFMF](https://github.com/NTIA/LFMF) provided by the US National Telecommunications and Information Administration [NTIA](https://www.ntia.gov).


The following table describes the structure of the folder `./src/` containing the Java implementation of Recommendation ITU-R P.368.

| File/Folder               | Description                                                         |
|----------------------------|---------------------------------------------------------------------|
|`main/P368.java`                | Java class implementing Recommendation ITU-R P.368-10         |
|`test/P368Test.java`          | Java class implementing validation tests against the reference/integral C++ implementation of this Recommendation for a range of input variables.          |



## Function Call

~~~ 
result = tl_p368(htx, hrx, f, Ptx, Ns, d, eps, sigma, pol); ;
~~~

## Required input arguments of function `tl_p368`

| Variable          | Type   | Units | Limits       | Description  |
|-------------------|--------|-------|--------------|--------------|
| `htx`               | scalar double | m   | 0 ≤ `htx` ≤ 50   | Height of the transmitter  |
| `hrx`      | scalar double | m    | 0 ≤ `hrx` ≤ 50 | Height of the receiver |
| `f`          | scalar double | MHz    | 0.01 ≤ `f` ≤ 30   | Frequency|
| `Ptx`          | scalar double | W    | 0 < `Ptx`    | Transmitter power|
| `Ns`          | scalar double |  N-units  | 250 ≤ `Ptx` ≤ 400    | Surface refractivity|
| `d`          | scalar double | km  | 0 < `d`    | Path distance|
| `eps`          | scalar double |    | 1 ≤ `eps`     | Relative permittivity|
| `sigma`          | scalar double |  S/m  | 0 ≤ `sigma`     | Conductivity|
| `pol`           | scalar int    |       |             |  Polarization <br> 0 = horizontal <br> 1 = vertical |



 
## Outputs ##

Outputs are contained within a defined `result` structure:

| Variable   | Type   | Units | Description |
|------------|--------|-------|-------------|
| `A_btl__db`    | double | dB    | Basic transmission loss |
| `E__dbuVm`    | double | dBuV/m    | Electric field strength |
| `P_rx__dbm`	| double  |	dBm	|Electromagnetic field power |
| `method`    | string |      | Method used <br> flat-earth curve <br> residue series |
| `error`    |  string |    | Error message|

## References

* [Recommendation ITU-R P.368](https://www.itu.int/rec/R-REC-P.368/en)

* [ITU-R SG 3 Software, Data, and Validation Web Page](https://www.itu.int/en/ITU-R/study-groups/rsg3/Pages/iono-tropo-spheric.aspx)

* [NTIA/LFMF](https://github.com/NTIA/LFMF) 

<!-- * [SEAMCAT - Spectrum Engineering Advanced Monte Carlo Analysis Tool](https://seamcat.org) -->
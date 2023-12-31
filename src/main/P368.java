package main;

// Translated and adapted to Java by Ivica Stevanovic (OFCOM CH)
// starting from the original C++ code by (NTIA, USA) available from https://github.com/NTIA/LFMF

import org.apache.commons.math3.complex.Complex;


// Recommendation ITU-R P.368

public class P368{
    // Class implementation of Recommendation ITU-R P.368-10
    //
    // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
    // EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
    // MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
    // IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
    // OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
    // ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE O;R
    // OTHER DEALINGS IN THE SOFTWARE.
    //
    // You may improve, modify, and create derivative works of the software or
    // any portion of the software, and you may copy and distribute such
    // modifications or works. Modified works should carry a notice stating
    // that you changed the software and should note the date and nature of
    // any such change.
    //
    // Please provide appropriate acknowledgments in any copies or
    // derivative works of this software.

    public static class Result {

        public double A_btl__db;
        double P_rx__dbm;
        double E_dBuVm;
        int method;
        int err;

        public Result() {
            this.A_btl__db = 0;
            P_rx__dbm = 0;
            E_dBuVm = 0;

            this.method = 0;
            this.err = 0;
        }
    };


    public static class Const {

        // Various constants definitions used by the code

        public static double epsilon_0 = 8.854187817e-12;         //Vacuum permittivity (F/m)
        public static double a_0__km = 6370.0;                    // Earth radius, in km
        public static double C =  299792458.0;             // Speed of light (m/s)
        public static double THIRD = 1.0 / 3.0;


        public static double ETA = 119.9169832*Math.PI;          // Intrinsic impedance of free space (ohms)

        // List of valid polarizations
        public static int POLARIZATION__HORIZONTAL = 0;
        public static int POLARIZATION__VERTICAL   = 1;

        public static int METHOD__FLAT_EARTH_CURVE  = 0;
        public static int METHOD__RESIDUE_SERIES    = 1;

        public static int YES    =  1;   // Find the derivative i.e., Ai'() or Bi'()
        public static int NO     =  0;   // Find Ai() or Bi()

        // kind
        public static int AIRY   =  1;   // Find the Airy Function
        public static int AIRYD  =  2;   // Find the Airy function Derivative
        public static int BAIRY  =  3;   // Find the Bairy Function
        public static int BAIRYD =  4;   // Find the Bairy function Derivative
        public static int WTWO   =  5;   // find Hufford Wi(2) or Wait W2
        public static int DWTWO  =  6;   // find Hufford Wi'(2) or Wait W2'
        public static int WONE   =  7;   // find Hufford Wi(1) or Wait W1
        public static int DWONE  =  8;   // find Hufford Wi'(1) or Wait W1'

        // scaling
        public static int HUFFORD=  9;   // Use Hufford scaling
        public static int WAIT   = 10;  // Use Wait scaling
        public static int NONE   = 11;  // No Scaling


        // Error codes

        public static int SUCCESS = 0;
        public static int ERROR__TX_TERMINAL_HEIGHT = 1000;    // TX terminal height is out of range
        public static int ERROR__RX_TERMINAL_HEIGHT = 1001;    // RX terminal height is out of range
        public static int ERROR__FREQUENCY = 1002;    // Frequency is out of range
        public static int ERROR__TX_POWER = 1003;    // Transmit power is out of range
        public static int ERROR__SURFACE_REFRACTIVITY = 1004;    // Surface refractivity is out of range
        public static int ERROR__PATH_DISTANCE = 1005;    // Path distance is out of range
        public static int ERROR__EPSILON = 1006;    // Epsilon is out of range
        public static int ERROR__SIGMA = 1007;    // Sigma is out of range
        public static int ERROR__POLARIZATION = 1008;    // Invalid value for polarization


    };


    public Complex Airy(Complex Z, int kind, int scaling) {
        /*=============================================================================
 |
 |       Author:  William Kozma Jr
 |                wkozma@ntia.gov
 |                US Dept of Commerce, NTIA/ITS
 |                June 2021 : Geneva Study Group 3 Meetings
 |
 |       Translation to Java: Ivica Stevanovic, OFCOM CH, November 2023
 |
 +-----------------------------------------------------------------------------
 |
 |  Description:  This routine finds the Airy, Bariy, Wi(1) and Wi(2)
 |                functions and their derivatives for a complex input argument
 |                from a shifted Taylor series or by asymptotic approximation
 |                depending of the location of the input argument.
 |
 |                This routine determines the so-called "Airy Functions of the
 |                third kind" Wi(1) and Wi(2) that are found in equation 38
 |                of NTIA Report 87-219 "A General Theory of Radio
 |                Propagation through a Stratified Atmosphere", George
 |                Hufford, July 1987
 |
 |                The Airy function that appeared in the original GWINT and
 |                GWRES had the switches all mangled from what George Hufford
 |                had in mind this routine has the corrected switches. Please
 |                see the Airy function code that appears in the appendix of
 |                OT/ITS RR 11 "A Wave Hop Propagation Program for an
 |                Anisotropic Ionosphere" L. A. Berry and J. E. Herman
 |                April 1971
 |
 |        Input:  Z             - Input argument
 |                kind          - Switch that indicates what type of Airy
 |                                function to solve for
 |                scaling       -
 |
 |      Outputs:  [None]
 |
 |      Returns:  Ai            - The desired Airy function calculated at Z
 |
 |         Note:  A note on scaling the output from this program
 |
 |               There is a definitional problem with the Airy function
 |               which is inevitable relative to how it was defined in the
 |               original LFMF code originated with the Hufford's AIRY
 |               subroutine.
 |
 |               Using the scaling equal to HUFFORD in this program follows
 |               the definitions of Wi(1) and Wi(2) as defined by Hufford
 |               (87-219)
 |
 |               Using the scaling equal to WAIT in this program uses the
 |               definitions of W1 and W2 defined in Deminco (99-368) and
 |               in the original LFMF code following Berry via Wait.
 |
 |               The two solutions differ by a constant. As Hufford notes
 |               concerning Wi(1) and Wi(2) in 87-219
 |
 |               "Except for multiplicative constants they correspond to
 |               what Fock (1965) calls w1 and w2 and to what Wait (1962)
 |               calls w2 and w1"
 |
 |               The following are the multiplicative constants that allow
 |               for the translation between Hufford Wi(2) and Wi(1) with
 |               Wait W1 and W2, respectively. These are given here as a
 |               reference if this function is used for programs other
 |               than LFMF.
 |
 |               // Wait
 |               complex<double> WW2  = complex<double>(     sqrt(3.0*PI),      sqrt(PI));
 |               complex<double> WDW2 = complex<double>(-1.0*sqrt(3.0*PI),      sqrt(PI));
 |               complex<double> WW1  = complex<double>(     sqrt(3.0*PI), -1.0*sqrt(PI));
 |               complex<double> WDW1 = complex<double>(-1.0*sqrt(3.0*PI), -1.0*sqrt(PI));
 |
 |               // Hufford
 |               complex<double> HW2  = 2.0*complex<double>(cos( PI/3.0), sin( PI/3.0));
 |               complex<double> HDW2 = 2.0*complex<double>(cos(-PI/3.0), sin(-PI/3.0));
 |               complex<double> HW1  = 2.0*complex<double>(cos(-PI/3.0), sin(-PI/3.0));
 |               complex<double> HDW1 = 2.0*complex<double>(cos( PI/3.0), sin( PI/3.0));
 |
 |               // (Multiplicative constant) * Huffords Wi'(1) = Wait W1'
 |               // So the multiplicative constants are:
 |               complex<double> uDW2 = WDW2/HDW1; // uDW2 = complex<double>(0.0,  sqrt(PI))
 |               complex<double> uW2  = WW2/HW1;   // uW2  = complex<double>(0.0,  sqrt(PI))
 |               complex<double> uDW1 = WDW1/HDW2; // uDW1 = complex<double>(0.0, -sqrt(PI))
 |               complex<double> uW1  = WW1/HW2;   // uW1  = complex<double>(0.0, -sqrt(PI))
 |
 |               To make the solutions that are generated by this program
 |               for the Hufford Airy functions of the "3rd kind" abundantly
 |               clear please examine the following examples.
 |
 |               For Z = 8.0 + 8.0 i the Asymptotic Solution is used
 |
 |               Ai( 8.0 + 8.0 i) =  6.576933e-007 +  9.312331e-006 i
 |               Ai'(8.0 + 8.0 i) =  9.79016e-006  + -2.992170e-005 i
 |               Bi( 8.0 + 8.0 i) = -1.605154e+003 + -4.807200e+003 i
 |               Bi'(8.0 + 8.0 i) =  1301.23 + -16956 i
 |               Wi(1)(8.0 + 8.0 i) = -4.807200e+003 +  1.605154e+003 i   !!!!
 |               Wi(2)(8.0 + 8.0 i) =  4.807200e+003 + -1.605154e+003 i   !!!!
 |               Ai(z) - j*Bi(z) = -4.807200e+003 +  1.605154e+003 i
 |               Ai(z) + j*Bi(z) =  4.807200e+003 + -1.605154e+003 i      !!!!
 |
 |               For Z = 1.0 - 2.0 i the Taylor series with a shifted
 |               center of expansion solution used.
 |
 |               Ai( 1.0 - 2.0 i) = -2.193862e-001 + 1.753859e-001 i
 |               Ai'(1.0 - 2.0 i) =  0.170445 + -0.387622 i
 |               Bi( 1.0 - 2.0 i) =  4.882205e-002 + -1.332740e-001 i
 |               Bi'(1.0 - 2.0 i) = -0.857239 + -0.495506 i
 |               Wi(1)(1.0 - 2.0 i) = -3.526603e-001 + 1.265638e-001 i    !!!!
 |               Wi(2)(1.0 - 2.0 i) = -8.611221e-002 + 2.242079e-001 i    !!!!
 |               Ai(z) - j*Bi(z) = -3.526603e-001 + 1.265639e-001 i
 |               Ai(z) + j*Bi(z) = -8.611221e-002 + 2.242080e-001 i       !!!!
 |
 *===========================================================================*/
        // NQTT, ASLT data
        int[] NQTT = {1, 3, 7, 12, 17, 23, 29, 35, 41, 47, 53, 59, 64, 68, 71}; // Centers of Expansion of Taylor series on real axis indices into the
        // AV, APV, BV and BPV arrays
        int N;              // Index into NQTT[] array
        int NQ8;            // Index that indicates the radius of convergence of the Taylor series solution
        int CoERealidx;     // Center of Expansion of the Taylor Series real index
        int CoEImagidx;     // Center of Expansion of the Taylor Series imaginary index
        int cnt;            // loop counter for the Taylor series calculation
        int derivative;     // index for derivative

        boolean reflection;    // Flag to indicate that the answer needs to be flipped over since this routine only finds solutions in quadrant 1 and 2

        Complex A[] = new Complex[2];
        A[0] = new Complex(0,0);
        A[1] = new Complex(0,0);
        Complex U = new Complex(0.0, 0.0);
        Complex ZB = new Complex(0.0, 0.0);
        Complex ZT, B0, B1, B2, B3, AN, AIRYW, ZA, ZE, ZR, V, ZV, ZU, PHZU; // Temps
        Complex CoE;        // Center of Expansion of the Taylor series
        Complex Ai;         // Ai is either Ai(at the center of expansion of the Taylor series) or Bi( at the center of expansion of the Taylor series )
        Complex Aip;        // Aip is the derivative of the above
        Complex sum1;       // Temp Sum for the asymptotic solution
        Complex sum2;       // Temp Sum for the asymptotic solution
        Complex ZB2, ZB1;

        double one;                 // Used in the calculation of the asymptotic solution is either -1 or 1

        // terms for asymptotic series.  second column is for derivative
        int SIZE_OF_ASV = 15;
        double[][] ASV = {{0.5989251E+5, -0.6133571E+5},
                {0.9207207E+4, -0.9446355E+4},
                {0.1533169E+4, -0.1576357E+4},
                {0.2784651E+3, -0.2870332E+3},
                {0.5562279E+2, -0.5750830E+2},
                {0.1234157E+2, -0.1280729E+2},
                {0.3079453E+1, -0.3210494E+1},
                {0.8776670E+0, -0.9204800E+0},
                {0.2915914E+0, -0.3082538E+0},
                {0.1160991E+0, -0.1241059E+0},
                {0.5764919E-1, -0.6266216E-1},
                {0.3799306E-1, -0.4246283E-1},
                {0.3713349E-1, -0.4388503E-1},
                {0.6944444E-1, -0.9722222E-1},
                {0.1000000E+1, 0.1000000E+1}};


        // Complex array of the value Ai(a) Airy function
        // where a is the complex location of the center of expansion of the Taylor series
        Complex AV[] = new Complex[70];

        // Complex array of the value Ai'(a) derivative of the Airy function
        // where a is the complex location of the center of expansion of the Taylor series
        // presumably for Ai'[z]
        Complex APV[] = new Complex[70];

        //////////////////////////////////////////////////////////////////////////
        // Initialize the center of expansion arrays.                           //
        //////////////////////////////////////////////////////////////////////////
        // The array AV[] (and BV[]) is the Airy function for Ai(a) to shift    //
        // the Taylor series from the origin to the point a Thus the series is  //
        // f(z -a)                                                              //
        // Why George Hufford choose these particular locations for the         //
        // of the centers of expansion is unknown. The centers of expansion are //
        // included here to remove any ambiguity in the method.                 //
        //////////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////   Center of expansion
        ///////////////////////////////////////////////////   ( Real, Imaginary)

        AV[0] = new Complex(-3.2914520e-001, +0.0000000e+000);//    (-6,0)
        AV[1] = new Complex(-2.6780040e+000, +1.4774590e+000);//    (-6,1/sin(pi/3))
        AV[2] = new Complex(+3.5076100e-001, +0.0000000e+000);//    (-5,0)
        AV[3] = new Complex(+2.4122260e+000, +6.9865120e-001);//    (-5,1/sin(pi/3))
        AV[4] = new Complex(+3.3635530e+001, -3.4600960e+000);//    (-5,2/sin(pi/3))
        AV[5] = new Complex(+3.4449740e+002, -3.3690890e+002);//    (-5,3/sin(pi/3))
        AV[6] = new Complex(-7.0265530e-002, +0.0000000e+000);//    (-4,0)
        AV[7] = new Complex(-5.4818220e-001, -1.9207370e+000);//    (-4,1/sin(pi/3))
        AV[8] = new Complex(-1.3383400e+001, -1.6022590e+001);//    (-4,2/sin(pi/3))
        AV[9] = new Complex(-2.2967800e+002, -3.2072450e+001);//    (-4,3/sin(pi/3))
        AV[10] = new Complex(-1.8040780e+003, +2.1917680e+003);//   (-4,4/sin(pi/3))
        AV[11] = new Complex(-3.7881430e-001, +0.0000000e+000);//   (-3,0)
        AV[12] = new Complex(-1.3491840e+000, +8.4969080e-001);//   (-3,1/sin(pi/3))
        AV[13] = new Complex(-6.0453340e+000, +1.0623180e+001);//   (-3,2/sin(pi/3))
        AV[14] = new Complex(+3.1169620e+001, +9.8813520e+001);//   (-3,3/sin(pi/3))
        AV[15] = new Complex(+9.8925350e+002, +1.3905290e+002);//   (-3,4/sin(pi/3))
        AV[16] = new Complex(+2.2740740e-001, +0.0000000e+000);//   (-2,0)
        AV[17] = new Complex(+7.1857400e-001, +9.7809090e-001);//   (-2,1/sin(pi/3))
        AV[18] = new Complex(+6.0621090e+000, +2.7203010e+000);//   (-2,2/sin(pi/3))
        AV[19] = new Complex(+3.6307080e+001, -2.0961360e+001);//   (-2,3/sin(pi/3))
        AV[20] = new Complex(-6.7139790e+001, -3.0904640e+002);//   (-2,4/sin(pi/3))
        AV[21] = new Complex(-2.8001650e+003, +4.6649370e+002);//   (-2,5/sin(pi/3))
        AV[22] = new Complex(+5.3556090e-001, +0.0000000e+000);//   (-1,0)
        AV[23] = new Complex(+9.2407370e-001, -1.9106560e-001);//   (-1,1/sin(pi/3))
        AV[24] = new Complex(+1.8716190e+000, -2.5743310e+000);//   (-1,2/sin(pi/3))
        AV[25] = new Complex(-7.2188440e+000, -1.2924200e+001);//   (-1,3/sin(pi/3))
        AV[26] = new Complex(-8.1787380e+001, +3.2087010e+001);//   (-1,4/sin(pi/3))
        AV[27] = new Complex(+2.9933950e+002, +5.6922180e+002);//   (-1,5/sin(pi/3))
        AV[28] = new Complex(+3.5502810e-001, +0.0000000e+000);//   ( 0,0)
        AV[29] = new Complex(+3.1203440e-001, -3.8845390e-001);//   ( 0,1/sin(pi/3))
        AV[30] = new Complex(-5.2840000e-001, -1.0976410e+000);//   ( 0,2/sin(pi/3))
        AV[31] = new Complex(-4.2009350e+000, +1.1940150e+000);//   ( 0,3/sin(pi/3))
        AV[32] = new Complex(+7.1858830e+000, +1.9600910e+001);//   ( 0,4/sin(pi/3))
        AV[33] = new Complex(+1.0129120e+002, -7.5951230e+001);//   ( 0,5/sin(pi/3))
        AV[34] = new Complex(+1.3529240e-001, +0.0000000e+000);//   ( 1,0)
        AV[35] = new Complex(+3.2618480e-002, -1.7084870e-001);//   ( 1,1/sin(pi/3))
        AV[36] = new Complex(-3.4215380e-001, -8.9067650e-002);//   ( 1,2/sin(pi/3))
        AV[37] = new Complex(-1.4509640e-001, +1.0328020e+000);//   ( 1,3/sin(pi/3))
        AV[38] = new Complex(+4.1001970e+000, -6.8936910e-001);//   ( 1,4/sin(pi/3))
        AV[39] = new Complex(-1.3030120e+001, -1.6910540e+001);//   ( 1,5/sin(pi/3))
        AV[40] = new Complex(+3.4924130e-002, +0.0000000e+000);//   ( 2,0)
        AV[41] = new Complex(-8.4464730e-003, -4.2045150e-002);//   ( 2,1/sin(pi/3))
        AV[42] = new Complex(-6.9313270e-002, +3.5364800e-002);//   ( 2,2/sin(pi/3))
        AV[43] = new Complex(+1.5227620e-001, +1.2848450e-001);//   ( 2,3/sin(pi/3))
        AV[44] = new Complex(+1.0681370e-001, -6.7766150e-001);//   ( 2,4/sin(pi/3))
        AV[45] = new Complex(-2.6193430e+000, +1.5699860e+000);//   ( 2,5/sin(pi/3))
        AV[46] = new Complex(+6.5911390e-003, +0.0000000e+000);//   ( 3,0)
        AV[47] = new Complex(-3.9443990e-003, -6.8060110e-003);//   ( 3,1/sin(pi/3))
        AV[48] = new Complex(-5.9820130e-003, +1.1799010e-002);//   ( 3,2/sin(pi/3))
        AV[49] = new Complex(+2.9922500e-002, -5.9772930e-003);//   ( 3,3/sin(pi/3))
        AV[50] = new Complex(-7.7464130e-002, -5.2292400e-002);//   ( 3,4/sin(pi/3))
        AV[51] = new Complex(+1.1276590e-001, +3.5112440e-001);//   ( 3,5/sin(pi/3))
        AV[52] = new Complex(+9.5156390e-004, +0.0000000e+000);//   ( 4,0)
        AV[53] = new Complex(-8.0843000e-004, -7.6590130e-004);//   ( 4,1/sin(pi/3))
        AV[54] = new Complex(+1.6147820e-004, +1.7661760e-003);//   ( 4,2/sin(pi/3))
        AV[55] = new Complex(+2.0138720e-003, -3.1976720e-003);//   ( 4,3/sin(pi/3))
        AV[56] = new Complex(-9.5086780e-003, +4.5377830e-003);//   ( 4,4/sin(pi/3))
        AV[57] = new Complex(+3.7560190e-002, +5.7361920e-004);//   ( 4,5/sin(pi/3))
        AV[58] = new Complex(+1.0834440e-004, +0.0000000e+000);//   ( 5,0)
        AV[59] = new Complex(-1.0968610e-004, -5.9902330e-005);//   ( 5,1/sin(pi/3))
        AV[60] = new Complex(+1.0778190e-004, +1.5771600e-004);//   ( 5,2/sin(pi/3))
        AV[61] = new Complex(-6.8980940e-005, -3.7626460e-004);//   ( 5,3/sin(pi/3))
        AV[62] = new Complex(-1.6166130e-004, +9.7457770e-004);//   ( 5,4/sin(pi/3))
        AV[63] = new Complex(+9.9476940e-006, +0.0000000e+000);//   ( 6,0)
        AV[64] = new Complex(-1.0956820e-005, -2.9508800e-006);//   ( 6,1/sin(pi/3))
        AV[65] = new Complex(+1.4709070e-005, +8.1042090e-006);//   ( 6,2/sin(pi/3))
        AV[66] = new Complex(-2.4446020e-005, -2.0638140e-005);//   ( 6,3/sin(pi/3))
        AV[67] = new Complex(+7.4921290e-007, +0.0000000e+000);//   ( 7,0)
        AV[68] = new Complex(-8.4619070e-007, -3.6807340e-008);//   ( 7,1/sin(pi/3))
        AV[69] = new Complex(+1.2183960e-006, +8.3589200e-008);//   ( 7,2/sin(pi/3))

        //////////////////////////////////////////////////////////////////////////
        // This array APV[] is the derivative of the Airy function for Ai'(a)   //
        //////////////////////////////////////////////////////////////////////////
        ///////////////////////////////////////////////////   Center of expansion
        ///////////////////////////////////////////////////   ( Real, Imaginary)
        APV[0] = new Complex(+3.4593550e-001, +0.0000000e+000);//   (-6,0)
        APV[1] = new Complex(+4.1708880e+000, +6.2414440e+000);//   (-6,1/sin(pi/3))
        APV[2] = new Complex(+3.2719280e-001, +0.0000000e+000);//   (-5,0)
        APV[3] = new Complex(+1.0828740e+000, -5.4928300e+000);//   (-5,1/sin(pi/3))
        APV[4] = new Complex(-2.3363520e+001, -7.4901850e+001);//   (-5,2/sin(pi/3))
        APV[5] = new Complex(-1.0264880e+003, -5.6707940e+002);//   (-5,3/sin(pi/3))
        APV[6] = new Complex(-7.9062860e-001, +0.0000000e+000);//   (-4,0)
        APV[7] = new Complex(-3.8085830e+000, +1.5129610e+000);//   (-4,1/sin(pi/3))
        APV[8] = new Complex(-2.6086380e+001, +3.5540710e+001);//   (-4,2/sin(pi/3))
        APV[9] = new Complex(+1.0761840e+002, +5.1239940e+002);//   (-4,3/sin(pi/3))
        APV[10] = new Complex(+6.6597800e+003, +1.8096190e+003);//  (-4,4/sin(pi/3))
        APV[11] = new Complex(+3.1458380e-001, +0.0000000e+000);//  (-3,0)
        APV[12] = new Complex(+1.8715430e+000, +2.0544840e+000);//  (-3,1/sin(pi/3))
        APV[13] = new Complex(+2.2591740e+001, +4.8563000e+000);//  (-3,2/sin(pi/3))
        APV[14] = new Complex(+1.6163000e+002, -1.4335600e+002);//  (-3,3/sin(pi/3))
        APV[15] = new Complex(-8.0047160e+002, -2.1527450e+003);//  (-3,4/sin(pi/3))
        APV[16] = new Complex(+6.1825900e-001, +0.0000000e+000);//  (-2,0)
        APV[17] = new Complex(+1.3019600e+000, -1.2290770e+000);//  (-2,1/sin(pi/3))
        APV[18] = new Complex(+1.5036120e-001, -1.1008090e+001);//  (-2,2/sin(pi/3))
        APV[19] = new Complex(-7.0116800e+001, -4.0480820e+001);//  (-2,3/sin(pi/3))
        APV[20] = new Complex(-4.8317170e+002, +4.9692760e+002);//  (-2,4/sin(pi/3))
        APV[21] = new Complex(+4.8970660e+003, +4.8627290e+003);//  (-2,5/sin(pi/3))
        APV[22] = new Complex(-1.0160570e-002, +0.0000000e+000);//  (-1,0)
        APV[23] = new Complex(-5.4826640e-001, -7.1365290e-001);//  (-1,1/sin(pi/3))
        APV[24] = new Complex(-4.6749130e+000, -1.1924250e-001);//  (-1,2/sin(pi/3))
        APV[25] = new Complex(-1.0536400e+001, +2.4943710e+001);//  (-1,3/sin(pi/3))
        APV[26] = new Complex(+1.6333770e+002, +9.0394910e+001);//  (-1,4/sin(pi/3))
        APV[27] = new Complex(+5.6449460e+002, -1.4248320e+003);//  (-1,5/sin(pi/3))
        APV[28] = new Complex(-2.5881940e-001, +0.0000000e+000);//  ( 0,0)
        APV[29] = new Complex(-4.8620750e-001, +1.5689920e-001);//  ( 0,1/sin(pi/3))
        APV[30] = new Complex(-4.7348130e-001, +1.7093440e+000);//  ( 0,2/sin(pi/3))
        APV[31] = new Complex(+7.0373840e+000, +3.6281820e+000);//  ( 0,3/sin(pi/3))
        APV[32] = new Complex(+1.7739590e+001, -4.0360420e+001);//  ( 0,4/sin(pi/3))
        APV[33] = new Complex(-2.9791510e+002, -3.8408890e+001);//  ( 0,5/sin(pi/3))
        APV[34] = new Complex(-1.5914740e-001, +0.0000000e+000);//  ( 1,0)
        APV[35] = new Complex(-1.1340420e-001, +1.9730500e-001);//  ( 1,1/sin(pi/3))
        APV[36] = new Complex(+4.0126210e-001, +3.9223000e-001);//  ( 1,2/sin(pi/3))
        APV[37] = new Complex(+1.3348650e+000, -1.4377270e+000);//  ( 1,3/sin(pi/3))
        APV[38] = new Complex(-7.9022490e+000, -4.2063640e+000);//  ( 1,4/sin(pi/3))
        APV[39] = new Complex(-1.3892750e+000, +5.1229420e+001);//  ( 1,5/sin(pi/3))
        APV[40] = new Complex(-5.3090380e-002, +0.0000000e+000);//  ( 2,0)
        APV[41] = new Complex(-1.6832970e-003, +6.8366970e-002);//  ( 2,1/sin(pi/3))
        APV[42] = new Complex(+1.3789400e-001, -1.1613800e-002);//  ( 2,2/sin(pi/3))
        APV[43] = new Complex(-1.4713730e-001, -3.7151990e-001);//  ( 2,3/sin(pi/3))
        APV[44] = new Complex(-1.0070200e+000, +1.1591350e+000);//  ( 2,4/sin(pi/3))
        APV[45] = new Complex(+7.5045050e+000, +4.6913120e-001);//  ( 2,5/sin(pi/3))
        APV[46] = new Complex(-1.1912980e-002, +0.0000000e+000);//  ( 3,0)
        APV[47] = new Complex(+5.1468570e-003, +1.3660890e-002);//  ( 3,1/sin(pi/3))
        APV[48] = new Complex(+1.8309710e-002, -1.8808590e-002);//  ( 3,2/sin(pi/3))
        APV[49] = new Complex(-6.4461590e-002, -1.3611790e-002);//  ( 3,3/sin(pi/3))
        APV[50] = new Complex(+1.0516240e-001, +1.9313050e-001);//  ( 3,4/sin(pi/3))
        APV[51] = new Complex(+2.0520050e-001, -9.1772620e-001);//  ( 3,5/sin(pi/3))
        APV[52] = new Complex(-1.9586410e-003, +0.0000000e+000);//  ( 4,0)
        APV[53] = new Complex(+1.4695650e-003, +1.8086380e-003);//  ( 4,1/sin(pi/3))
        APV[54] = new Complex(+5.9709950e-004, -3.8332700e-003);//  ( 4,2/sin(pi/3))
        APV[55] = new Complex(-6.8910890e-003, +5.4467430e-003);//  ( 4,3/sin(pi/3))
        APV[56] = new Complex(+2.6167930e-002, -8.4092000e-004);//  ( 4,4/sin(pi/3))
        APV[57] = new Complex(-8.8284470e-002, -4.6475310e-002);//  ( 4,5/sin(pi/3))
        APV[58] = new Complex(-2.4741390e-004, +0.0000000e+000);//  ( 5,0)
        APV[59] = new Complex(+2.3707840e-004, +1.6461110e-004);//  ( 5,1/sin(pi/3))
        APV[60] = new Complex(-1.7465570e-004, -4.2026780e-004);//  ( 5,2/sin(pi/3))
        APV[61] = new Complex(-1.0394520e-004, +9.4761840e-004);//  ( 5,3/sin(pi/3))
        APV[62] = new Complex(+1.3004110e-003, -2.2446660e-003);//  ( 5,4/sin(pi/3))
        APV[63] = new Complex(-2.4765200e-005, +0.0000000e+000);//  ( 6,0)
        APV[64] = new Complex(+2.6714870e-005, +9.8691570e-006);//  ( 6,1/sin(pi/3))
        APV[65] = new Complex(-3.3539770e-005, -2.7113280e-005);//  ( 6,2/sin(pi/3))
        APV[66] = new Complex(+4.9197840e-005, +6.9349090e-005);//  ( 6,3/sin(pi/3))
        APV[67] = new Complex(-2.0081510e-006, +0.0000000e+000);//  ( 7,0)
        APV[68] = new Complex(+2.2671240e-006, +2.7848510e-007);//  ( 7,1/sin(pi/3))
        APV[69] = new Complex(-3.2692130e-006, -7.3943490e-007);//  ( 7,2/sin(pi/3))

        // Complex array of the value Bi(a) Airy function
        // where a is the complex location of the center of expansion of the Taylor series
        Complex BV[] = new Complex[70];

        // Complex array of the value Bi'(a) derivative of the Airy function
        // where a is the complex location of the center of expansion of the Taylor series
        // presumably for Ai'[z]
        Complex BPV[] = new Complex[70];

        /////////////////////////////////////////////////////////////////////////

        BV[0] = new Complex(-1.466984e-001, -9.813078e-017);//  (-6,0)
        BV[1] = new Complex(-1.489391e+000, -2.660635e+000);//  (-6,1/sin(pi/3))
        BV[2] = new Complex(-1.383691e-001, +0.000000e+000);//  (-5,0)
        BV[3] = new Complex(-7.034482e-001, +2.384547e+000);//  (-5,1/sin(pi/3))
        BV[4] = new Complex(+3.460723e+000, +3.363363e+001);//  (-5,2/sin(pi/3))
        BV[5] = new Complex(+3.369090e+002, +3.444973e+002);//  (-5,3/sin(pi/3))
        BV[6] = new Complex(+3.922347e-001, -1.041605e-016);//  (-4,0)
        BV[7] = new Complex(+1.956219e+000, -5.327226e-001);//  (-4,1/sin(pi/3))
        BV[8] = new Complex(+1.602464e+001, -1.338050e+001);//  (-4,2/sin(pi/3))
        BV[9] = new Complex(+3.207239e+001, -2.296777e+002);//  (-4,3/sin(pi/3))
        BV[10] = new Complex(-2.191768e+003, -1.804078e+003);// (-4,4/sin(pi/3))
        BV[11] = new Complex(-1.982896e-001, +4.440892e-016);// (-3,0)
        BV[12] = new Complex(-8.880754e-001, -1.308713e+000);// (-3,1/sin(pi/3))
        BV[13] = new Complex(-1.062975e+001, -6.044056e+000);// (-3,2/sin(pi/3))
        BV[14] = new Complex(-9.881405e+001, +3.116914e+001);// (-3,3/sin(pi/3))
        BV[15] = new Complex(-1.390528e+002, +9.892534e+002);// (-3,4/sin(pi/3))
        BV[16] = new Complex(-4.123026e-001, +1.451806e-016);// (-2,0)
        BV[17] = new Complex(-1.034766e+000, +6.541962e-001);// (-2,1/sin(pi/3))
        BV[18] = new Complex(-2.720266e+000, +6.048328e+000);// (-2,2/sin(pi/3))
        BV[19] = new Complex(+2.096300e+001, +3.630613e+001);// (-2,3/sin(pi/3))
        BV[20] = new Complex(+3.090465e+002, -6.713963e+001);// (-2,4/sin(pi/3))
        BV[21] = new Complex(-4.664937e+002, -2.800165e+003);// (-2,5/sin(pi/3))
        BV[22] = new Complex(+1.039974e-001, +0.000000e+000);// (-1,0)
        BV[23] = new Complex(+2.797458e-001, +8.086491e-001);// (-1,1/sin(pi/3))
        BV[24] = new Complex(+2.606133e+000, +1.870297e+000);// (-1,2/sin(pi/3))
        BV[25] = new Complex(+1.292648e+001, -7.213647e+000);// (-1,3/sin(pi/3))
        BV[26] = new Complex(-3.208774e+001, -8.178697e+001);// (-1,4/sin(pi/3))
        BV[27] = new Complex(-5.692218e+002, +2.993394e+002);// (-1,5/sin(pi/3))
        BV[28] = new Complex(+6.149266e-001, +0.000000e+000);// ( 0,0)
        BV[29] = new Complex(+6.732023e-001, +3.575876e-001);// ( 0,1/sin(pi/3))
        BV[30] = new Complex(+1.125057e+000, -4.471292e-001);// ( 0,2/sin(pi/3))
        BV[31] = new Complex(-1.211148e+000, -4.191469e+000);// ( 0,3/sin(pi/3))
        BV[32] = new Complex(-1.960240e+001, +7.182663e+000);// ( 0,4/sin(pi/3))
        BV[33] = new Complex(+7.595175e+001, +1.012911e+002);// ( 0,5/sin(pi/3))
        BV[34] = new Complex(+1.207424e+000, +0.000000e+000);// ( 1,0)
        BV[35] = new Complex(+5.951440e-001, +6.156664e-001);// ( 1,1/sin(pi/3))
        BV[36] = new Complex(-1.002325e-001, -1.338228e-001);// ( 1,2/sin(pi/3))
        BV[37] = new Complex(-1.089323e+000, -2.019524e-001);// ( 1,3/sin(pi/3))
        BV[38] = new Complex(+7.047139e-001, +4.091592e+000);// ( 1,4/sin(pi/3))
        BV[39] = new Complex(+1.691067e+001, -1.302705e+001);// ( 1,5/sin(pi/3))
        BV[40] = new Complex(+3.298095e+000, +0.000000e+000);// ( 2,0)
        BV[41] = new Complex(+2.244706e-001, +2.421124e+000);// ( 2,1/sin(pi/3))
        BV[42] = new Complex(-1.199515e+000, -1.167656e-001);// ( 2,2/sin(pi/3))
        BV[43] = new Complex(+6.781072e-003, -2.225418e-001);// ( 2,3/sin(pi/3))
        BV[44] = new Complex(+7.470822e-001, +1.832986e-001);// ( 2,4/sin(pi/3))
        BV[45] = new Complex(-1.590993e+000, -2.617694e+000);// ( 2,5/sin(pi/3))
        BV[46] = new Complex(+1.403733e+001, +0.000000e+000);// ( 3,0)
        BV[47] = new Complex(-3.731398e+000, +1.066394e+001);// ( 3,1/sin(pi/3))
        BV[48] = new Complex(-4.440986e+000, -4.309647e+000);// ( 3,2/sin(pi/3))
        BV[49] = new Complex(+2.373933e+000, -5.300179e-001);// ( 3,3/sin(pi/3))
        BV[50] = new Complex(-2.821481e-001, +5.657373e-001);// ( 3,4/sin(pi/3))
        BV[51] = new Complex(-3.904913e-001, -5.168316e-002);// ( 3,5/sin(pi/3))
        BV[52] = new Complex(+8.384707e+001, +0.000000e+000);// ( 4,0)
        BV[53] = new Complex(-4.356467e+001, +5.497027e+001);// ( 4,1/sin(pi/3))
        BV[54] = new Complex(-7.156364e+000, -4.113550e+001);// ( 4,2/sin(pi/3))
        BV[55] = new Complex(+1.455852e+001, +1.109071e+001);// ( 4,3/sin(pi/3))
        BV[56] = new Complex(-6.111359e+000, -1.094609e-001);// ( 4,4/sin(pi/3))
        BV[57] = new Complex(+1.403434e+000, -7.255043e-001);// ( 4,5/sin(pi/3))
        BV[58] = new Complex(+6.577920e+002, +0.000000e+000);// ( 5,0)
        BV[59] = new Complex(-4.598656e+002, +3.242259e+002);// ( 5,1/sin(pi/3))
        BV[60] = new Complex(+1.324505e+002, -3.294705e+002);// ( 5,2/sin(pi/3))
        BV[61] = new Complex(+2.057579e+001, +1.674034e+002);// ( 5,3/sin(pi/3))
        BV[62] = new Complex(-3.161505e+001, -5.302141e+001);// ( 5,4/sin(pi/3))
        BV[63] = new Complex(+6.536446e+003, +0.000000e+000);// ( 6,0)
        BV[64] = new Complex(-5.316522e+003, +1.992175e+003);// ( 6,1/sin(pi/3))
        BV[65] = new Complex(+2.888529e+003, -2.373473e+003);// ( 6,2/sin(pi/3))
        BV[66] = new Complex(-1.078657e+003, +1.551931e+003);// ( 6,3/sin(pi/3))
        BV[67] = new Complex(+8.032779e+004, +0.000000e+000);// ( 7,0)
        BV[68] = new Complex(-7.001987e+004, +8.828416e+003);// ( 7,1/sin(pi/3))
        BV[69] = new Complex(+4.676699e+004, -1.085924e+004);// ( 7,2/sin(pi/3))

        //////////////////////////////////////////////////////////////

        BPV[0] = new Complex(-8.128988e-001, +3.365185e-016);//     (-6,0)
        BPV[1] = new Complex(-6.287609e+000, +4.146176e+000);//     (-6,1/sin(pi/3))
        BPV[2] = new Complex(+7.784118e-001, -3.004629e-016);//     (-5,0)
        BPV[3] = new Complex(+5.554036e+000, +1.063645e+000);//     (-5,1/sin(pi/3))
        BPV[4] = new Complex(+7.490659e+001, -2.336310e+001);//     (-5,2/sin(pi/3))
        BPV[5] = new Complex(+5.670796e+002, -1.026488e+003);//     (-5,3/sin(pi/3))
        BPV[6] = new Complex(-1.166706e-001, +2.371654e-015);//     (-4,0)
        BPV[7] = new Complex(-1.532413e+000, -3.730947e+000);//     (-4,1/sin(pi/3))
        BPV[8] = new Complex(-3.554558e+001, -2.608034e+001);//     (-4,2/sin(pi/3))
        BPV[9] = new Complex(-5.124001e+002, +1.076185e+002);//     (-4,3/sin(pi/3))
        BPV[10] = new Complex(-1.809619e+003, +6.659780e+003);//    (-4,4/sin(pi/3))
        BPV[11] = new Complex(-6.756112e-001, -2.403703e-017);//    (-3,0)
        BPV[12] = new Complex(-2.142202e+000, +1.818610e+000);//    (-3,1/sin(pi/3))
        BPV[13] = new Complex(-4.863137e+000, +2.258023e+001);//    (-3,2/sin(pi/3))
        BPV[14] = new Complex(+1.433564e+002, +1.616285e+002);//    (-3,3/sin(pi/3))
        BPV[15] = new Complex(+2.152746e+003, -8.004716e+002);//    (-3,4/sin(pi/3))
        BPV[16] = new Complex(+2.787952e-001, +0.000000e+000);//    (-2,0)
        BPV[17] = new Complex(+1.300360e+000, +1.185229e+000);//    (-2,1/sin(pi/3))
        BPV[18] = new Complex(+1.103082e+001, +1.397575e-001);//    (-2,2/sin(pi/3))
        BPV[19] = new Complex(+4.048422e+001, -7.011484e+001);//    (-2,3/sin(pi/3))
        BPV[20] = new Complex(-4.969277e+002, -4.831712e+002);//    (-2,4/sin(pi/3))
        BPV[21] = new Complex(-4.862729e+003, +4.897066e+003);//    (-2,5/sin(pi/3))
        BPV[22] = new Complex(+5.923756e-001, +0.000000e+000);//    (-1,0)
        BPV[23] = new Complex(+9.080502e-001, -5.080757e-001);//    (-1,1/sin(pi/3))
        BPV[24] = new Complex(+1.499485e-001, -4.631403e+000);//    (-1,2/sin(pi/3))
        BPV[25] = new Complex(-2.494926e+001, -1.052676e+001);//    (-1,3/sin(pi/3))
        BPV[26] = new Complex(-9.039663e+001, +1.633370e+002);//    (-1,4/sin(pi/3))
        BPV[27] = new Complex(+1.424833e+003, +5.644943e+002);//    (-1,5/sin(pi/3))
        BPV[28] = new Complex(+4.482884e-001, +0.000000e+000);//    ( 0,0)
        BPV[29] = new Complex(+2.493288e-002, -1.876446e-001);//    ( 0,1/sin(pi/3))
        BPV[30] = new Complex(-1.774795e+000, -3.533830e-001);//    ( 0,2/sin(pi/3))
        BPV[31] = new Complex(-3.663891e+000, +7.026174e+000);//    ( 0,3/sin(pi/3))
        BPV[32] = new Complex(+4.036322e+001, +1.773235e+001);//    ( 0,4/sin(pi/3))
        BPV[33] = new Complex(+3.840990e+001, -2.979143e+002);//    ( 0,5/sin(pi/3))
        BPV[34] = new Complex(+9.324359e-001, +0.000000e+000);//    ( 1,0)
        BPV[35] = new Complex(-1.293870e-001, +7.817697e-001);//    ( 1,1/sin(pi/3))
        BPV[36] = new Complex(-8.385825e-001, +4.901385e-001);//    ( 1,2/sin(pi/3))
        BPV[37] = new Complex(+1.421331e+000, +1.181168e+000);//    ( 1,3/sin(pi/3))
        BPV[38] = new Complex(+4.244380e+000, -7.895016e+000);//    ( 1,4/sin(pi/3))
        BPV[39] = new Complex(-5.123410e+001, -1.383387e+000);//    ( 1,5/sin(pi/3))
        BPV[40] = new Complex(+4.100682e+000, +0.000000e+000);//    ( 2,0)
        BPV[41] = new Complex(-9.576171e-001, +3.432468e+000);//    ( 2,1/sin(pi/3))
        BPV[42] = new Complex(-1.747487e+000, -8.602854e-001);//    ( 2,2/sin(pi/3))
        BPV[43] = new Complex(+9.978890e-001, -6.434913e-001);//    ( 2,3/sin(pi/3))
        BPV[44] = new Complex(-1.127841e+000, -7.762214e-001);//    ( 2,4/sin(pi/3))
        BPV[45] = new Complex(-5.136144e-001, +7.476880e+000);//    ( 2,5/sin(pi/3))
        BPV[46] = new Complex(+2.292221e+001, +0.000000e+000);//    ( 3,0)
        BPV[47] = new Complex(-1.021000e+001, +1.662556e+001);//    ( 3,1/sin(pi/3))
        BPV[48] = new Complex(-5.018884e+000, -1.067168e+001);//    ( 3,2/sin(pi/3))
        BPV[49] = new Complex(+5.067979e+000, +1.074279e+000);//    ( 3,3/sin(pi/3))
        BPV[50] = new Complex(-1.620678e+000, +1.029461e+000);//    ( 3,4/sin(pi/3))
        BPV[51] = new Complex(+1.055970e+000, -2.041230e-001);//    ( 3,5/sin(pi/3))
        BPV[52] = new Complex(+1.619267e+002, +0.000000e+000);//    ( 4,0)
        BPV[53] = new Complex(-1.021827e+002, +9.434616e+001);//    ( 4,1/sin(pi/3))
        BPV[54] = new Complex(+9.638391e+000, -8.764529e+001);//    ( 4,2/sin(pi/3))
        BPV[55] = new Complex(+2.157904e+001, +3.568852e+001);//    ( 4,3/sin(pi/3))
        BPV[56] = new Complex(-1.346647e+001, -6.665778e+000);//    ( 4,4/sin(pi/3))
        BPV[57] = new Complex(+4.276735e+000, -9.657825e-002);//    ( 4,5/sin(pi/3))
        BPV[58] = new Complex(+1.435819e+003, +0.000000e+000);//    ( 5,0)
        BPV[59] = new Complex(-1.099383e+003, +5.897525e+002);//    ( 5,1/sin(pi/3))
        BPV[60] = new Complex(+4.709887e+002, -6.717565e+002);//    ( 5,2/sin(pi/3))
        BPV[61] = new Complex(-7.965464e+001, +4.040825e+002);//    ( 5,3/sin(pi/3))
        BPV[62] = new Complex(-2.418965e+001, -1.582957e+002);//    ( 5,4/sin(pi/3))
        BPV[63] = new Complex(+1.572560e+004, +0.000000e+000);//    ( 6,0)
        BPV[64] = new Complex(-1.334470e+004, +3.525428e+003);//    ( 6,1/sin(pi/3))
        BPV[65] = new Complex(+8.229089e+003, -4.446372e+003);//    ( 6,2/sin(pi/3))
        BPV[66] = new Complex(-3.795705e+003, +3.141147e+003);//    ( 6,3/sin(pi/3))
        BPV[67] = new Complex(+2.095527e+005, +0.000000e+000);//    ( 7,0)
        BPV[68] = new Complex(-1.853403e+005, +7.452520e+003);//    ( 7,1/sin(pi/3))
        BPV[69] = new Complex(+1.286235e+005, -8.069218e+003);//    ( 7,2/sin(pi/3))

        ////////////////////////////////////////
        // Validate input - kind & derivative //
        ////////////////////////////////////////
        if ((kind < 1) || (kind > 8)) {
            return new Complex(0, 0);   // Airy Error: Invalid kind value
        }


        if ((scaling != Const.NONE) && (scaling != Const.HUFFORD) && (scaling != Const.WAIT)) {
            return new Complex(0, 0);   // Airy Error: Invalid scaling value
        }

        // Set the derivative flag
        derivative = Const.NO;

        if (kind == Const.DWTWO || kind == Const.DWONE || kind == Const.AIRYD || kind == Const.BAIRYD) {
            derivative = Const.YES;
        }


        // Now do something productive with numbers...

        // In the original version of this by George Hufford it calculated Ai(Z), Ai'(Z) and the
        // Airy functions of the "3rd kind" Wi(1)(Z) and Wi(2)(Z) (see Eqn 38 in NTIA Report 87-219)
        // Note: Theta in 87-219 is Z in this program.
        // The input switch had three values, here we are going to have four so that the
        // Bairy function doesn't feel left out.

        // The following scales the input parameter Z depending on what the user is trying to do.
        // If the user is trying to find just the Ai(Z), Ai'(Z), Bi(Z) or Bi'(Z) there is no scaling.
        if (kind == Const.AIRY || kind == Const.BAIRY || kind == Const.AIRYD || kind == Const.BAIRYD) {
            // For Ai(Z) and  Bi(Z) No translation in the complex plane
            U = new Complex(1.0, 0.0);
        }
        // Note that W1 Wait = Wi(2) Hufford and W2 Wait = Wi(1) Hufford
        // So the following inequalities keep this all straight
        else if (((kind == Const.DWONE || kind == Const.WONE) && scaling == Const.HUFFORD)
                ||
                ((kind == Const.DWTWO || kind == Const.WTWO) && scaling == Const.WAIT)) {
            // This corresponds to Wi(1)(Z) in Eqn 38 Hufford NTIA Report 87-219
            // or Wait W2
            U = new Complex(Math.cos(2.0 * Math.PI / 3.0), Math.sin(2.0 * Math.PI / 3.0));
        } else if (((kind == Const.DWTWO || kind == Const.WTWO) && scaling == Const.HUFFORD)
                ||
                ((kind == Const.DWONE || kind == Const.WONE) && scaling == Const.WAIT)) {
            // This corresponds to Wi(2)(Z) in Eqn 38 Hufford NTIA Report 87-219
            // or Wait W1
            U = new Complex(Math.cos(-2.0 * Math.PI / 3.0), Math.sin(-2.0 * Math.PI / 3.0));
        }

        // Translate the input parameter
        //ZU = Z * U;
        ZU = Z.multiply(U);

        // We will be only calculating for quadrant 1 and 2. If the desired value is in 3 or 4 we
        // will have to flip it over after the calculation
        reflection = false;
        if (ZU.getImaginary() <= 0) {
            reflection = true; // reflection = true means Z.imag() <= 0, use reflection formula to get result
            ZU = new Complex(ZU.getReal(), -ZU.getImaginary());
        }

        // Begin the calculation to determine if
        //          a) the shifted Taylor series will be used or
        //          b) the Asymptotic approximation is used.
        // A shifted Taylor series is necessary because the Taylor series is defined with a center of expansion
        // at the origin is a poor approximation to the true value of the (B)Airy function.

        // NOTE: The condition for which method is used is dependant on that value of Z and not the
        // transformed version of ZU, which is the shifted Z by exp(+-j*2*pi/3).
        // For the calculation of Ai(Z) and Bi(Z) Z is not shifted.
        // For the calculation of Wi(1)(Z) and Wi(2)(Z) the value of Ai(ZU) is found.

        // Initialize the indexes for the center of expansion
        // Note these are used in the if statements below as flags
        N = 0;
        NQ8 = 0;

        // If Z is small, use Taylor's series at various centers of expansion chosen by George Hufford
        // If Z is large, use Asymptotic series NIST DLMF 9.4.5 - 9.4.8

        // The following inequality is formed from the implicit arguments for the AV[], BV[], BPV[] and APV[]
        // The inequality makes sure that the center of expansion for the Taylor series solution is not
        // exceeded.
        //      (ZU.real() >= -6.5) -6.5 is 0.5 is the real value of the center of expansion in the array
        //      (ZU.real() <= 7.5)   7.5 is 0.5 is the real value of the center of expansion in the array
        //      (ZU.imag() <= 6.35) 6.35 is 5.5/sin(PI/3) which is 0.5 past 5/sin(PI/3)
        if ((ZU.getReal() >= -6.5) && (ZU.getReal() <= 7.5) && (ZU.getImaginary() <= 6.35)) {

            // choose center of expansion of the Taylor series
            CoERealidx = (int) (ZU.getReal() + Math.copySign(0.5, ZU.getReal()));
            CoEImagidx = (int) (Math.sin(Math.PI / 3.0) * (ZU.getImaginary() + 0.5)); // sin(60)*(Z.imag()+0.5)

            N = NQTT[CoERealidx + 6] + CoEImagidx;  // N is index of center of expansion

            // Check to see if N is out of bounds
            if (N >= 70) { // Stop if the index N reaches the limit of array AV[] which is 70
                //printf("Airy() Error: Z is too large\n");
                return new Complex(0, 0);
            }

            NQ8 = NQTT[CoERealidx + 7];             // The next real center of expansion or what is know here ...
            // as the area of the Taylor's series

            // if Z is inside Taylor's series area, continue. Otherwise, go to asymptotic series
            if (N < NQ8) {

                ///////////////////////////////////////////
                // Compute the function by Taylor Series //
                ///////////////////////////////////////////

                // sum Taylor's series around nearest expansion point
                // The arrays AV[] and APV[] are incremented in the complex domain by 1/sin(PI/3)
                CoE = new Complex((double) CoERealidx, (double) CoEImagidx / Math.sin(Math.PI / 3.0));

                // Translate the input parameter to the new location
                //ZU = ZU - CoE;
                ZU = ZU.subtract(CoE);

                // Calculate the first term of the Taylor Series
                // To do this we need to find the Airy or Bairy function at the center of
                // expansion, CoE, that has been precalculated in the arrays above.
                if (kind == Const.BAIRY || kind == Const.BAIRYD) {
                    Ai = BV[N - 1];         // Bi(CoE)
                    Aip = BPV[N - 1];       // Bi'(CoE)
                } else { // All other cases use the Coe for Ai(z)
                    Ai = AV[N - 1];         // Ai(CoE)
                    Aip = APV[N - 1];       // Ai'(CoE)
                }


                // Find the first elements of the Taylor series
                //                                                       Translation
                B1 = Ai;            // B1 is first term for function        Ai(a)
                //B3 = B1 * CoE*ZU;   // B3 is second term for derivative     Ai(a)*a*(z-a)
                B3 = B1.multiply(CoE).multiply(ZU);   // B3 is second term for derivative     Ai(a)*a*(z-a)

                A[1] = Aip;         // A is first term for derivation       Ai'(a)
                //B2 = A[1] * ZU;     // B2 is second term for function       Ai'(a)(z-a)
                B2 = A[1].multiply(ZU);
                //A[0] = B2 + B1;     // A[0] is the sum of Ai() or Bi()      Ai'(a)(z-a) + Ai(a)
                A[0] = B2.add(B1);
                //A[1] = A[1] + B3;   // A[1] is the sum of Ai'() or Bi'()    Ai'(a) + Ai(a)*a*(z-a)
                A[1] = A[1].add(B3);
                AN = new Complex(1.0, 0.0);

                // Initialize the counter
                cnt = 0;

                // compute terms of series and sum until convergence
                while (cnt < 3) { // require that the loop be executed 3 times
                    while (true) {
                        AN = AN.add(1.0);
                        B3 = B3.multiply(ZU.divide(AN));
                        A[0] = B3.add(A[0]);
                        B0 = B1;
                        B1 = B2;
                        B2 = B3;
                        //  B3 = (CoE*B1 + ZU * B0)*ZU / AN;
                        B3 = (((CoE.multiply(B1)).add(ZU.multiply(B0))).multiply(ZU)).divide(AN);

                        A[1] = B3.add(A[1]);

                        // Has the convergence criteria been met?
                        if ((B2.abs() <= (0.5E-7 * A[0].abs())) && (B3.abs() <= (0.5E-7 * A[1].abs()))) {
                            break;
                        }

                    }
                    cnt = cnt + 1;

                }
            }

        } // if ((ZU.real() >= -6.5) && (ZU.real() <= 7.5) && (ZU.imag() <= 6.35))


        // Determine if the data for the center of expansion is exceeded
        if (((ZU.getReal() < -6.5) || (ZU.getReal() > 7.5) || (ZU.getImaginary() > 6.35))
                ||
                (N >= NQ8)
                ||
                (Z.getReal() == 0 && Z.getImaginary() == 0)) {


            ///////////////////////////////////////////////
            // Compute the function by Asymptotic Series //
            ///////////////////////////////////////////////

            ///////////////////////////////////////////////////////
            // Please see                                        //
            // "On the Asymptotic Expansion of Airy's Integral"  //
            // E. T. Copson, Cambridge Press, November, 1962     //
            // for details on why a second series is necessary   //
            // The equations found in the reference above appear //
            // to be what Hufford used in the creation of this   //
            // algorithm                                         //
            ///////////////////////////////////////////////////////

            // Find intermediate values
            //ZA = sqrt(ZU);              // zeta^(1/2)
            ZA = ZU.sqrt();
            //ZT = (2.0 / 3.0)*ZU*ZA;     // NIST DLMF 9.7.1 => -(2/3)zeta^(3/2)
            ZT = ZU.multiply(ZA).multiply(2.0 / 3.0);

            if (kind == Const.BAIRY || kind == Const.BAIRYD) {
                one = 1.0;  // Terms for the Bairy sum do not alternate sign
            } else {
                one = -1.0; // All other functions use the Airy whose sum alternates sign
            }

            // Compute the asymptotic series either sum over k for (u_k*zeta^-k) or sum over k for (v_k*zeta^-k)
            // Which is used depends on M => M = 0 use u_k M = 1 use v_k
            // By doing this backward you don't have to do multiple powers zeta^-1
            // Note the coefficients are backward so the for loop will be forward
            sum1 = new Complex(0.0, 0.0); // Initialize the sum
            for (int i = 0; i < 14; i++) {
                // sum1 = (pow(one, i)*ASV[i][derivative] + sum1) / ZT;
                sum1 = sum1.add(Math.pow(one, i) * ASV[i][derivative]).divide(ZT);
            }
            sum1 = sum1.add(ASV[SIZE_OF_ASV - 1][derivative]); // Add the first element that is a function of zeta^0

            // Now determine if a second series is necessary
            // If it is not set the second sum to zero

            ////////////////////////////////////////////////////////////////////////////////////
            // Historic Note:
            // Hufford originally used the following inequality in AIRY()
            // (See OT/ITS RR 11) IF(XT(2) .GT. 0. .AND. XT(l) .LT. ll.8595) LG=4
            // to determine if a second series is required for a reasonable level of accuracy.
            // The C translation for the variables defined here
            // would be if(ZT.imag() <= 0.0) && (ZT.real() >= -ll.8595)
            // In the LFMF code the inequality for the same purpose is (translated to C)
            // if((ZT.imag() <= 0.0) && (ZT.real() >= -8.4056))
            // Since ZT = (2/3)ZU^(3/2) and for ZT.imag() = 0.0 and ZT.real() = -8.4056
            // ZU = -2.70859033 + j*4.69141606 which has an angle of 2*PI/3
            // Similarly for Hufford's original code
            // ZU = -3.40728475 + j*5.90159031 which has an angle of 2*PI/3
            // Thus we could replace the following with the inequality
            // if(ZU.arg() > 2.0*PI/3.0)
            //////////////////////////////////////////////////////////////////////////////////////

            // From Copson the F(z) solution is only valid for phase(z) <= PI/3.0
            // While the F(z) + i*G(z) solution is necessary for phase(z) > PI/3.0
            if (Math.abs(ZU.getArgument()) > Math.PI / 3.0) {
                sum2 = new Complex(0.0, 0.0); // Initialize the second sum
                for (int i = 0; i < 14; i++) {
                    sum2 = (sum2.add(ASV[i][derivative])).divide(ZT);
                }
                sum2 = sum2.add(ASV[SIZE_OF_ASV - 1][derivative]); // Add the first element that is a function of zeta^0
            } else { // Only one series is necessary for accuracy
                sum2 = new Complex(0.0, 0.0);
            }
            ;

            // Now do the final function that leads the sum depending on what the user wants.
            // The leading function has to be taken apart so that it can be assembled as necessary for
            // the possible two parts of the sum
            if (kind == Const.BAIRY || kind == Const.BAIRYD) {
                if (derivative == Const.NO) {
                    //ZB = 1.0 / (sqrt(ZA));          // NIST DLMF 9.7.8
                    ZB = (new Complex(1.0, 0.0)).divide(ZA.sqrt());
                } else if (derivative == Const.YES) {
                    //ZB = sqrt(ZA);                  // NIST DLMF 9.7.7
                    ZB = ZA.sqrt();
                }
                // ZB1 = ZB * exp(ZT) / sqrt(PI);      // For Bairy multiply by e^(zeta)/sqrt(PI)
                ZB1 = (ZB.multiply(ZT.exp())).divide(Math.sqrt(Math.PI));

                //ZB2 = ZB * 1.0 / (exp(ZT)*sqrt(PI));
                ZB2 = ZB.divide((ZT.exp()).multiply(Math.sqrt(Math.PI)));

            } else { // All other kind use Airy
                if (derivative == Const.NO) {
                    //ZB = 1.0 / sqrt(ZA);            // NIST DLMF 9.7.6
                    ZB = (new Complex(1.0, 0.0)).divide(ZA.sqrt());
                } else if (derivative == Const.YES) {
                    //ZB = -1.0*sqrt(ZA);             // NIST DLMF 9.7.5
                    ZB = ZA.sqrt().multiply(-1.0);
                }
                //ZB1 = ZB * 1.0 / (2.0*exp(ZT)*sqrt(PI));    // For Airy multiply be e^(-zeta)/(2.0*sqrt(PI))
                ZB1 = ZB.divide((ZT.exp()).multiply(2.0 * Math.sqrt(Math.PI)));
                //ZB2 = ZB * exp(ZT) / (2.0*sqrt(PI));
                ZB2 = ZB.multiply(ZT.exp().divide(2.0 * Math.sqrt(Math.PI)));
            }


            // Multiply by the leading coefficient to get the results for NIST DLMF 9.7.5 - 9.7.8
            if (derivative == Const.YES) {
                //A[derivative] = ZB1 * sum1 - complex<double>(0.0, 1.0)*ZB2*sum2;
                A[derivative] = ZB1.multiply(sum1).subtract(new Complex(0.0, 1.0).multiply(ZB2).multiply(sum2));
            } else if (derivative == Const.NO) {
                //A[derivative] = ZB1 * sum1 + complex<double>(0.0, 1.0)*ZB2*sum2;
                A[derivative] = ZB1.multiply(sum1).add(new Complex(0.0, 1.0).multiply(ZB2).multiply(sum2));
            }


        }  // if (( Z.real() < 6.5 || Z.real() > 7.5 || Z.imag() > 6.35 || N > NQ8 || ((Z.real() == 0 && Z.imag() == 0))))


        //////////////////////////////////////////////
        // End of the Asymptotic Series Calculation //
        //////////////////////////////////////////////

        // Store the desired quantity

        Ai = A[derivative];

        // Final Transform to get the desired function
        // Was the input parameter in quadrant 3 or 4?
        // If it was we have to take the conjugate of the calculation result
        if (reflection != false) {
            Ai = new Complex(Ai.getReal(), -Ai.getImaginary());
        }


        // The final scaling factor is a function of the kind, derivative and scaling flags
        if (scaling == Const.NONE) {
            // The number from the Taylor series or asymptotic calculation
            // does not need to multiplied by anything
            U = new Complex(1.0, 0.0);
        }
        // Hufford Wi(1) and Wi'(1)
        else if ((kind == Const.WONE || kind == Const.DWONE) && (scaling == Const.HUFFORD)) {
            if (derivative == Const.NO) {
                U = (new Complex(Math.cos(-Math.PI / 3.0), Math.sin(-Math.PI / 3.0))).multiply(2.0);
            } else if (derivative == Const.YES) {
                U = (new Complex(Math.cos(Math.PI / 3.0), Math.sin(Math.PI / 3.0))).multiply(2.0);
            }
        }
        // Hufford Wi(2) and Wi'(2)
        else if ((kind == Const.WTWO || kind == Const.DWTWO) && (scaling == Const.HUFFORD)) {
            if (derivative == Const.NO) {
                U = (new Complex(Math.cos(Math.PI / 3.0), Math.sin(Math.PI / 3.0))).multiply(2.0);
            } else if (derivative == Const.YES) {
                U = (new Complex(Math.cos(-Math.PI / 3.0), Math.sin(-Math.PI / 3.0))).multiply(2.0);
            }
        }
        // Wait W1 and W1'
        else if ((kind == Const.WONE || kind == Const.DWONE) && (scaling == Const.WAIT)) {
            if (derivative == Const.NO) {
                U = new Complex(Math.sqrt(3.0 * Math.PI), -1.0 * Math.sqrt(Math.PI));
            } else if (derivative == Const.YES) {
                U = new Complex(-1.0 * Math.sqrt(3.0 * Math.PI), -1.0 * Math.sqrt(Math.PI));
            }
        }
        // Wait W2 and W2'
        else if ((kind == Const.WTWO || kind == Const.DWTWO) && (scaling == Const.WAIT)) {
            if (derivative == Const.NO) {
                U = new Complex(Math.sqrt(3.0 * Math.PI), Math.sqrt(Math.PI));
            } else if (derivative == Const.YES) {
                U = new Complex(-1.0 * Math.sqrt(3.0 * Math.PI), Math.sqrt(Math.PI));
            }
        }

        // Scale the return value
        Ai = Ai.multiply(U);



        return Ai;

    }


    Complex wofz(Complex z)
    {

//     ALGORITHM 680, COLLECTED ALGORITHMS FROM ACM.
//     THIS WORK PUBLISHED IN TRANSACTIONS ON MATHEMATICAL SOFTWARE,
//     VOL. 16, NO. 1, PP. 47.
//
// GIVEN A COMPLEX NUMBER Z = (XI,YI), THIS SUBROUTINE COMPUTES
// THE VALUE OF THE FADDEEVA-FUNCTION W(Z) = EXP(-Z**2)*ERFC(-I*Z),
// WHERE ERF%IS THE COMPLEX COMPLEMENTARY ERROR-FUNCTION AND I
// MEANS SQRT(-1).
// THE ACCURACY OF THE ALGORITHM FOR Z IN THE 1ST AND 2ND QUADRANT
// IS 14 SIGNIFICANT DIGITS; IN THE 3RD AND 4TH IT IS 13 SIGNIFICANT
// DIGITS OUTSIDE A CIRCULAR REGION WITH RADIUS 0.126 AROUND A ZERO
// OF THE FUNCTION.
// ALL REAL VARIABLES IN THE PROGRAM ARE DOUBLE PRECISION.
//
//
// THE CODE CONTAINS A FEW COMPILER-DEPENDENT PARAMETERS :
//    RMAXREAL = THE MAXIMUM VALUE OF RMAXREAL EQUALS THE ROOT OF
//               RMAX = THE LARGEST NUMBER WHICH CAN STILL BE
//               IMPLEMENTED ON THE COMPUTER IN DOUBLE PRECISION
//               FLOATING-POINT ARITHMETIC
//    RMAXEXP  = LN(RMAX) - LN(2)
//    RMAXGONI = THE LARGEST POSSIBLE ARGUMENT OF A DOUBLE PRECISION
//               GONIOMETRI%FUNCTION (DCOS, DSIN, ...)
// THE REASON WHY THESE PARAMETERS ARE NEEDED AS THEY ARE DEFINED WILL
// BE EXPLAINED IN THE CODE BY MEANS OF COMMENTS
//
// PARAMETER LIST
//    flag   = AN ERROR FLAG INDICATING WHETHER OVERFLOW WILL
//             OCCUR OR NOT; TYPE LOGICAL;
//             THE VALUES OF THIS VARIABLE HAVE THE FOLLOWING
//             MEANING :
//             FLAG=.FALSE. : NO ERROR CONDITION
//             FLAG=.TRUE.  : OVERFLOW WILL OCCUR, THE ROUTINE
//                            BECOMES INACTIVE
//
// FURTHERMORE THE PARAMETER FACTOR EQUALS 2/SQRT(PI)
//
// THE ROUTINE IS NOT UNDERFLOW-PROTECTED BUT ANY VARIABLE CAN BE
// PUT TO 0 UPON UNDERFLOW;
//
// REFERENCE - GPM POPPE, CMJ WIJERS; MORE EFFICIENT COMPUTATION OF
// THE COMPLEX ERROR-FUNCTION, ACM TRANS. MATH. SOFTWARE.
//
// Translation to Java: Ivica Stevanovic, OFCOM CH, November 2023


        double FACTOR   = 1.12837916709551257388;
        double RMAXREAL = 0.5E+154;
        double RMAXEXP  = 708.503061461606E0;
        double RMAXGONI = 3.53711887601422E+15;

        double XI = z.getReal();
        double YI = z.getImaginary();

        double XABS = Math.abs(XI);
        double YABS = Math.abs(YI);
        double X    = XABS/6.3;
        double Y    = YABS/4.4;

        boolean flag = false;

        Complex w;

        double XSUM, YSUM, U, V, XAUX, U1, V1, DAUX,  H,  KAPN;
        double RX, RY, TX, TY, SX, SY, W1, CC;
        double H2 = 0.0;
        double QLAMBDA = 0.0;
        double U2 = 0.0;
        double V2 = 0.0;

        int NU, NP1;

//    THE FOLLOWING IF-STATEMENT PROTECTS
//    QRHO = (X**2 + Y**2) AGAINST OVERFLOW

        if ((XABS > RMAXREAL) || (YABS > RMAXREAL)) {
            flag = true;
            w = new Complex(0.0,0.0);
            return w;
        }

        double QRHO = X*X + Y*Y;

        double XABSQ = Math.pow(XABS,2.0);
        double XQUAD = XABSQ - Math.pow(YABS, 2.0);
        double YQUAD = 2*XABS*YABS;

        boolean A     = (QRHO < 0.085264E0);

        if (A) {

            // IF (QRHO < 0.085264) THEN THE FADDEEVA-FUNCTION IS EVALUATED
            // USING A POWER-SERIES (ABRAMOWITZ/STEGUN, EQUATION (7.1.5), P.297)
            // N IS THE MINIMUM NUMBER OF TERMS NEEDED TO OBTAIN THE REQUIRED
            // ACCURACY

            QRHO  = (1-0.85*Y)*Math.sqrt(QRHO);
            int N     = (int)(6 + 72*QRHO);
            int J     = 2*N+1;
            XSUM  = 1.0/J;
            YSUM  = 0.0;
            for (int I = N; I > 0; I--) {

                J    = J - 2;
                XAUX = (XSUM*XQUAD - YSUM*YQUAD)/I;
                YSUM = (XSUM*YQUAD + YSUM*XQUAD)/I;
                XSUM = XAUX + 1.0/J;
            }
            U1   = -FACTOR*(XSUM*YABS + YSUM*XABS) + 1.0;
            V1   =  FACTOR*(XSUM*XABS - YSUM*YABS);
            DAUX =  Math.exp(-XQUAD);
            U2   =  DAUX*Math.cos(YQUAD);
            V2   = -DAUX*Math.sin(YQUAD);

            U    = U1*U2 - V1*V2;
            V    = U1*V2 + V1*U2;

        } else {

            // IF (QRHO > 1.O) THEN W(Z) IS EVALUATED USING THE LAPLACE
            // CONTINUED FRACTION
            // NU IS THE MINIMUM NUMBER OF TERMS NEEDED TO OBTAIN THE REQUIRED
            // ACCURACY

            // IF ((QRHO > 0.085264).AND.(QRHO < 1.0)) THEN W(Z) IS EVALUATED
            // BY A TRUNCATED TAYLOR EXPANSION, WHERE THE LAPLACE CONTINUED FRACTION
            // IS USED TO CALCULATE THE DERIVATIVES OF W(Z)
            // KAPN IS THE MINIMUM NUMBER OF TERMS IN THE TAYLOR EXPANSION NEEDED
            // TO OBTAIN THE REQUIRED ACCURACY
            // NU IS THE MINIMUM NUMBER OF TERMS OF THE CONTINUED FRACTION NEEDED
            // TO CALCULATE THE DERIVATIVES WITH THE REQUIRED ACCURACY


            if (QRHO > 1.0) {
                H    = 0.0;
                KAPN = 0;
                QRHO = Math.sqrt(QRHO);
                NU   = (int)(3 + (1442.0/(26*QRHO+77)));
            } else {
                QRHO = (1-Y)*Math.sqrt(1-QRHO);
                H    = 1.88*QRHO;
                H2   = 2*H;
                KAPN = (int)(7  + 34*QRHO);
                NU   = (int)(16 + 26*QRHO);
            }

            boolean B = (H > 0.0);

            if (B) {
                QLAMBDA = Math.pow(H2, KAPN);
            }

            RX = 0.0;
            RY = 0.0;
            SX = 0.0;
            SY = 0.0;

            for (int N = NU; N>= 0; N--){
                NP1 = N + 1;
                TX  = YABS + H + NP1*RX;
                TY  = XABS - NP1*RY;
                CC   = 0.5/(TX*TX + TY*TY);
                RX  = CC*TX;
                RY  = CC*TY;
                if ((B) && (N <= KAPN)) {
                    TX = QLAMBDA + SX;
                    SX = RX*TX - RY*SY;
                    SY = RY*TX + RX*SY;
                    QLAMBDA = QLAMBDA/H2;
                }
            }

            if (H  == 0.0) {
                U = FACTOR*RX;
                V = FACTOR*RY;
            }else{
                U = FACTOR*SX;
                V = FACTOR*SY;
            }

            if (YABS == 0.0){
                U = Math.exp(-XABS*XABS);
            }
        }

// EVALUATION OF W(Z) IN THE OTHER QUADRANTS


        if (YI < 0.0) {

            if (A) {
                U2    = 2*U2;
                V2    = 2*V2;
            } else {
                XQUAD =  -XQUAD;


                //        THE FOLLOWING IF-STATEMENT PROTECTS 2*EXP(-Z**2)
                //        AGAINST OVERFLOW

                if ((YQUAD > RMAXGONI) || (XQUAD > RMAXEXP)) {
                    flag = true;
                    w = new Complex(0.0, 0.0);
                    return w;
                }


                W1 =  2*Math.exp(XQUAD);
                U2  =  W1*Math.cos(YQUAD);
                V2  = -W1*Math.sin(YQUAD);
            }

            U = U2 - U;
            V = V2 - V;
            if (XI > 0.0) {
                V = -V;
            }

        } else {
            if (XI < 0.0){
                V = -V;
            }
        }

        w = new Complex(U, V);
        return w;

    }


    Complex[] WiRoot(int i, Complex DWi, Complex q, Complex Wi, int kind, int scaling)

    /*=============================================================================
     |
     |       Author:  William Kozma Jr
     |                wkozma@ntia.gov
     |                US Dept of Commerce, NTIA/ITS
     |                June 2021 : Geneva Study Group 3 Meetings
     |
     |       Translation to Java: Ivica Stevanovic, OFCOM CH, November 2023
     |
     +-----------------------------------------------------------------------------
     |
     |  Description:  This routine finds the roots to the equation
     |
     |                  Wi'(ti) - q*Wi(ti) = 0
     |
     |                The parameter i selects the ith root of the equation. The
     |                function Wi(ti) is the "Airy function of the third kind"
     |                as defined by Hufford[1] and Wait. The root is found by
     |                iteration starting from a real root.
     |
     |                Note: Although roots that are found for W1 (Wait) and
     |                Wi(2) (Hufford) will be equal, and the roots found for
     |                W2 (Wait) and Wi(1) (Hufford) will be equal, the return
     |                values for *Wi and *DWi will not be the same. The input
     |                parameters for kind and scale are used here as they
     |                are in Airy() for consistency.
     |
     |   References:  "Airy Functions of the third kind" are found in equation 38
     |                  of NTIA Report 87-219 "A General Theory of Radio
     |                  Propagation through a Stratified Atmosphere", George
     |                  Hufford, July 1987
     |
     |        Input:  i             - The ith complex root of
     |                                Wi'(2)(ti) - q*Wi(2)(ti) starting with 1
     |                DWi           - Derivative of "Airy function of the
     |                                third kind" Wi'(2)(ti)
     |                q             - Intermediate value -j*nu*delta
     |                Wi            - "Airy function of the third kind" Wi(2)(ti)
     |                kind          - Kind of Airy function to use
     |                scaling       - Type of scaling to use
     |
     |      Outputs:  DWi           - Derivative of "Airy function of the
     |                                third kind" Wi'(2)(ti)
     |                Wi            - "Airy function of the third kind" Wi(2)(ti)
     |
     |      Returns:  tw            - ith complex root of the "Airy function of
     |                                the third kind"
     |
     *===========================================================================*/



    {
        Complex ph = new Complex(0,0);             // Airy root phase
        Complex ct;             // Temp
        Complex ti;             // the ith complex root of Wi'(2)(ti) - q*Wi(2)(ti) = 0

        Complex tw;             // Return variable
        Complex T;              // Temp
        Complex A;              // Temp

        double t, tt;                   // Temp
        double eps;                     // Temp

        int cnt;                        // Temp
        int dkind = 0;                      // Temp

        Complex[] out = new Complex[3];
        out[0] = new Complex(0.0, 0.0);
        out[1] = DWi;
        out[2] = Wi;
        // From the NIST DLMF (Digital Library of Mathematical Functions)
        // http://dlmf.nist.gov/
        // The first 10 roots of Ai(Z) and Ai'(Z) can be found in: Table 9.9.1: Zeros of Ai and Ai.
        // Note: That ak is the root of Ai(ak) while akp (ak') is the root of Ai'(akp)

        // Root of the Airy function, Ai(ak)
        // TZERO(I) in GWINT.FOR
        double[] akp = { -1.0187929716,
                -3.2481975822,
                -4.8200992112,
                -6.1633073556,
                -7.3721772550,
                -8.4884867340,
                -9.5354490524,
                -10.5276603970,
                -11.4750666335,
                -12.3847883718,
                -13.2636395229 };

        // Root of the derivative of Airy function, Ai'(akp)
        // TINFIN(I) in GWINT.FOR
        double[] ak = { -2.3381074105,
                -4.0879494441,
                -5.5205698281,
                -6.7867080901,
                -7.9441335871,
                -9.0226508533,
                -10.0401743416,
                -11.0085243037,
                -11.9360255632,
                -12.8287867529,
                -13.6914890352 };

        // Verify that the input data is correct
        // Make sure that the desired root is greater than or equal to one
        if (i <= 0)
        {
            // There is an input parameter error; printf("WiRoot(): The root must be >= 0 (%d)\n", i);
            tw = new Complex(-998.8, -998.8);
            out[0] = tw;
            return out;
        };

        if ((scaling != Const.HUFFORD) && (scaling != Const.WAIT))
        {
            // There is an input parameter error; printf("WiRoot(): The scaling must be HUFFORD (%d) or WAIT (%d)\n", HUFFORD, WAIT);
            tw = new Complex(-997.7, -997.7);
            out[0] = tw;
            return out;
        };

        if ((kind != Const.WTWO) && (kind != Const.WONE))
        {
            // There is an input parameter error; printf("WiRoot(): The kind must be W1 (%d) or W2 (%d)\n", WONE, WTWO);
            tw = new Complex(-996.6, -996.6);
            out[0] = tw;
            return out;
        };
        // Input parameters verified

        // Initialize the Wi and Wi'(z)functions
        DWi = new Complex(0.0, 0.0);   // Wi'(z)
        Wi = new Complex(0.0, 0.0);    // Wi(z)

        // This routine starts with a real root of the Airy function to find the complex root
        // The real root has to be turned into a complex number.

        // ph is a factor that is used to find the root of the Wi function
        // Determine what scaling the user wants and which Wi function is used to set ph and
        // the dkind flag. This will allow that the real root that starts this process can be
        // scaled appropriately.
        // This is the simmilar to the initial scaling that is done in Airy()
        // Note that W1 Wait = Wi(2) Hufford and W2 Wait = Wi(1) Hufford
        // So the following inequalities keep this all straight
        if ((kind == Const.WONE && scaling == Const.HUFFORD) || (kind == Const.WTWO && scaling == Const.WAIT))
        {
            // Wi(1)(Z) in Eqn 38 Hufford NTIA Report 87-219 or Wait W2
            ph = new Complex(Math.cos(-2.0*Math.PI / 3.0), Math.sin(-2.0*Math.PI / 3.0));
            // Set the dkind flag
            if (scaling == Const.WAIT)
            {
                dkind = Const.DWTWO;
            }
            else
            {
                dkind = Const.DWONE;
            }
        }
        else if ((kind == Const.WTWO && scaling == Const.HUFFORD) || (kind == Const.WONE && scaling == Const.WAIT))
        {
            // Wi(2)(Z) in Eqn 38 Hufford NTIA Report 87-219 or Wait W1
            ph = new Complex(Math.cos(2.0*Math.PI / 3.0), Math.sin(2.0*Math.PI / 3.0));
            if (scaling == Const.WAIT)
            {
                dkind = Const.DWONE;
            }
            else
            {
                dkind = Const.DWTWO;
            }
        }


        // Note: The zeros of the Airy functions i[ak'] and Ak'[ak], ak' and ak, are on the negative real axis.
        // This is why 4*i+3 and 4*i+1 are used here instead of 4*k-3 and 4*k-1 which are
        // used in 9.9.8 and 9.9.6 in NIST DLMF. We are finding the ith negative root here.
        if (Math.pow(q.abs(), 3.0) <= 4.0 * (i - 1.0) + 3.0) {
            // Small Z, use ak' as the first guess (Ak(ak') = 0)
            if (i <= 10)
            {
                // The desired root is less than 10 so it is in the array above
                tt = akp[i - 1];
            }
            else
            {
                // The desired root is a higher order than those given in the ak array above
                // so we will approximate it from the first three terms of NIST DLMF 9.9.1.9
                // First find the argument (9.9.8) used in 9.9.1.9 for the ith negative root of Ai'(ak).
                t = (3.0 / 8.0)*Math.PI*(4.0*(i - 1) + 1.0);
                tt = -1.0*Math.pow(t, 2.0 / 3.0)*(1.0 - ((7.0 / 48.0)*Math.pow(t, -2.0)) + ((35.0 / 288.0)*Math.pow(t, -4.0)));
            }
            // Make the real Airy root complex
            ti = ph.multiply(tt);
            // t is now the solution for q = 0, The next step is the first Newton iteration
            ti = ti.add(q.divide(ti));
        }
        else
        {
            // Large q, use ak as the first guess (Ai'(ak) = 0)
            if (i <= 10)
            {
                // The desired root is less than 10 so it is in the array above
                tt = ak[i - 1];
            }
            else
            {
                // The desired root must be approximated from the first three terms of NIST DLMF 9.9.1.8
                // First find the argument (9.9.6) used in 9.9.1.8 for the ith negative root of Ai(ak).
                t = (3.0 / 8.0)*Math.PI*(4.0*(i - 1) + 3.0);
                tt = -1.0*Math.pow(t, 2.0 / 3.0)*(1.0 + ((5.0 / 48.0)*Math.pow(t, -2.0)) - ((5.0 / 36.0)*Math.pow(t, -4.0)));
            }
            ti = ph.multiply(tt);
            // t is now the solution for Z = infinity. Next step the first newton iteration
            ti = ti.add( (new Complex(1.0,0.0 )).divide(q)) ;
        }

        cnt = 0;        // Set the iteration counter
        eps = 0.5e-6;   // Set the error desired for the iteration

        // Now iterate by Newton's method

        //////////////////////////////////////////////////////////////////////
        // Note: We can use the following from
        // Berry and Christman "The Path Integrals of LF/VLF Wave Hop Theory"
        // Radio Science Vol. 69D, No. 11 , November 1965
        // Eqn (14) E(t)  = W2'(t) - q W2(t)
        // Eqn (39) E'(t) = t W2(t) - q W2'(t)
        //////////////////////////////////////////////////////////////////////

        while(true) {

            // f(q) = Wi'(ti) - q*Wi(ti)
            Wi = Airy(ti, kind, scaling);

            // f'(q) = tw*Wi(ti) - q*Wi'(ti);
            DWi = Airy(ti, dkind, scaling);
            // The Newton correction factor for iteration f(q)/f'(q)
            //A = (DWi - q * (Wi)) / (ti*(Wi) - q * (DWi));
            A = (DWi.subtract(q.multiply(Wi))).divide((ti.multiply(Wi)).subtract(q.multiply(DWi)));
            ti = ti.subtract(A);    // New root guess ti
            cnt = cnt + 1;          // Increment the counter


            if ((cnt <= 25) && (Math.abs((A.divide(ti)).getReal()) + Math.abs((A.divide(ti)).getImaginary()) > eps)) {
                continue;
            }
            else {
                break;
            }


        }

        // Check to see if there if the loop converged on an answer
        if (cnt == 26) // The cnt that fails is an arbitrary number most converge in ~5 tries
        {
            // No Convergence return 0 + j*0 as the root as TW() did
            tw = new Complex(0.0, 0.0);
        }
        else
        {
            // Converged!
            tw = ti;
        };

        out[0] = tw;
        out[1] = DWi;
        out[2] = Wi;
        return out;
    }



    double ResidueSeries(double d__km, double k, double h_1__km, double h_2__km, double nu, double theta__rad, Complex q)
     /*=============================================================================
     |
     |       Author:  William Kozma Jr
     |                wkozma@ntia.gov
     |                US Dept of Commerce, NTIA/ITS
     |                June 2021 : Geneva Study Group 3 Meetings
     |
     |       Translation to Java: Ivica Stevanovic, OFCOM CH, November 2023
     |
     +-----------------------------------------------------------------------------
     |
     |  Description:  Calculates the groundwave field strength using the
     |                Residue Series method
     |
     |        Input:  d__km         - Path distance, in km
     |                k             - Wavenumber, in rad/km
     |                h_1__km       - Height of the lower antenna, in km
     |                h_2__km       - Height of the higher antenna, in km
     |                nu            - Intermediate value,
     |                                pow(a_e__km * k / 2.0, THIRD);
     |                theta__rad    - Angular distance of path, in radians
     |                q             - Intermediate value -j*nu*delta
     |
     |      Returns:  E_gw          - Normalized field strength in mV/m
     |
     *===========================================================================*/
    {
        Complex[] DW2 = new Complex[200];
        Complex[] W2  = new Complex[200]; // dummy variables
        Complex G;

        Complex j = new Complex(0.0, 1.0);

        Complex[] T  = new Complex[200];
        Complex[] W1 = new Complex[200];
        Complex[] W  = new Complex[200];
        double yHigh, yLow;

        Complex[] out = new Complex[3];

        Complex GW = new Complex(0, 0); // initial ground wave

        // Associated argument for the height-gain function H_1[h_1]
        yHigh = k * h_2__km / nu;

        // Associated argument for the height-gain function H_2[h_2]
        yLow = k * h_1__km / nu;

        double x = nu * theta__rad;

        for (int i = 0; i < 200; i++)
        {

            out = WiRoot(i + 1, DW2[i], q, W2[i], Const.WONE, Const.WAIT); // find the (i+1)th root of Airy function for given q
            T[i] = out[0];
            DW2[i] = out[1];
            W2[i] = out[2];

            W1[i] = Airy(T[i], Const.WONE, Const.WAIT); // Airy function of (i)th root

            if (h_1__km > 0)
            {
                W[i] = ( Airy(T[i].subtract(yLow), Const.WONE, Const.WAIT) ).divide( W1[i] ); //height gain function H_1(h_1) eqn.(22) from NTIA report 99-368

                if (h_2__km > 0)
                    W[i] = W[i].multiply( (Airy(T[i].subtract(yHigh), Const.WONE, Const.WAIT)).divide( W1[i] ) ); //H_1(h_1)*H_1(h_2)
            }
            else if (h_2__km > 0)
                W[i] = ( Airy(T[i].subtract(yHigh), Const.WONE, Const.WAIT) ).divide( W1[i] );
            else
                W[i] = new Complex(1, 0);

            // W[i] is the coefficient of the distance factor for the i-th
            W[i] = W[i].divide( (T[i].subtract(q.pow(2.0))) ); // H_1(h_1)*H_1(h_2)/(t_i-q^2) eqn.26 from NTIA report 99-368

            G = W[i].multiply( ( j.multiply(-x).multiply(T[i]) ).exp()); // sum of exp(-j*x*t_i)*W[i] eqn.26 from NTIA report 99-368
            GW = GW.add(G); // sum the series

            if (i != 0)
            {
                if (((Math.abs((GW.pow(2.0)).getReal())) + (Math.abs((GW.pow(2.0)).getImaginary()))) == 0)     // when the ground wave is too small, close to 0
                    return 0; // end the loop and output E = 0
                else if (((Math.abs((G.divide(GW)).getReal())) + (Math.abs((G.divide(GW)).getImaginary()))) < 0.0005)  // when the new G is too small compared to its series sum
                {
                    // when the new G is too small compared to its series sum, it's ok to stop the loop
                    // because adding small number to a significant big one doesn't affect their sum.
                    //J1 = i;
                    break;
                }
            }
        }

        // field strength.  complex<double>(sqrt(PI/2)) = sqrt(pi)*e(-j*PI/4)
        Complex Ew = (new Complex(Math.sqrt(Math.PI / 2.0), -Math.sqrt(Math.PI / 2.0)) ).multiply(GW).multiply(Math.sqrt(x));

        double E_gw = Ew.abs(); // take the magnitude of the result

        return E_gw;

    }



    double FlatEarthCurveCorrection(Complex Delta, Complex q, double h_1__km, double h_2__km, double d__km, double k, double a_e__km)
        /*=============================================================================
     |
     |       Author:  William Kozma Jr
     |                wkozma@ntia.gov
     |                US Dept of Commerce, NTIA/ITS
     |                June 2021 : Geneva Study Group 3 Meetings
     |
     |       Translation to Java: Ivica Stevanovic, OFCOM CH, November 2023
     |
     +-----------------------------------------------------------------------------
     |
     |  Description:  Calculates the groundwave field strength using the flat Earth
     |                approximation with curvature correction.
     |
     |   References:  99-368 "Medium Frequency Propagation
     |                  Prediction Techniques and Antenna Modeling for
     |                  Intelligent Transportation Systems (ITS) Broadcast
     |                  Applications", Nicholas DeMinco.  Eq (31)
     |                J. Wait, "Radiation From a Vertical Antenna Over a Curved
     |                  Stratified Ground", Journal of Research of the National
     |                  Bureau of Standards Vol 56, No. 4, April 1956
     |                  Research Paper 2671
     |
     |        Input:  Delta         - Surface impedance
     |                q             - Intermediate value -j*nu*delta
     |                h_1__km       - Height of the higher antenna, in km
     |                h_2__km       - Height of the lower antenna, in km
     |                d__km         - Path distance, in km
     |                k             - Wavenumber, in rad/km
     |                a_e__km       - Effective earth radius, in km
     |
     |      Returns:  E_gw          - Normalized field strength in mV/m
     |
     *===========================================================================*/
    {
        Complex j = new Complex(0.0, 1.0);

        // In order for the wofz() function to be used both here and in gwfe()
        // the argument, qi, has to be defined correctly. The following is how
        // it is done in the original GWFEC.FOR
        Complex qi = (new Complex(-0.5, 0.5) ).multiply(Delta).multiply(Math.sqrt(k*d__km));
        Complex p = qi.pow(2);

        Complex p2 = p.pow(2);
        Complex q3 = q.pow(3);
        Complex q6 = q.pow(6);
        Complex q9 = q.pow(9);

        Complex fofx;

        if((q).abs() > 0.1){
            // Find F(p) Eqn (32) NTIA Report 99-368
            Complex Fofp = j.multiply(qi).multiply(wofz(qi)).multiply( Math.sqrt(Math.PI) ).add(1.0);

            // Calculate f(x) which is the normalized electric field, E_ratio; Eqn (31) NTIA Report 99-368
            //fofx = Fofp + (1.0 - j * Math.sqrt(Math.PI * p) - (1.0 + 2.0 * p) * Fofp) / (4.0 * q3);
            fofx = Fofp.add( ( ( ( j.multiply(-1.0).multiply( (p.multiply(Math.PI)).sqrt() ) ).add(1.0) ).subtract( (p.multiply(2.0).add(1.0)).multiply(Fofp) ) ).divide(q3.multiply(4.0)) );
            //fofx = fofx + (1.0 - j * Math.sqrt(Math.PI * p) * (1.0 - p) - 2.0 * p + 5.0 * p2 / 6.0 + (p2 / 2.0 - 1.0) * Fofp) / (4.0 * q6);
            fofx = fofx.add( ( (( j.multiply(-1)).multiply(p.multiply(Math.PI).sqrt()).multiply(p.multiply(-1).add(1.0)) ).add(1.0).subtract(p.multiply(2.0)).add(p2.multiply(5.0/6.0)).add( ( p2.divide(2.0).subtract(1.0) ).multiply(Fofp) )).divide(q6.multiply(4.0)) );

        } else {

            Complex[] A = new Complex[10];

            // [Deminco, Eq 30]
            A[0] = new Complex(1.0, 0.0);
            A[1] = j.multiply(-1).multiply(Math.sqrt(Math.PI));
            A[2] = new Complex(-2.0,0.0);
            //A[3] = j * sqrt(PI) * (1.0 + 1.0/(4.0*q3));
            A[3] = j.multiply(Math.sqrt(Math.PI)).multiply( ((new Complex(1.0,0.0)).divide( q3.multiply(4.0) )).add(1.0) );
            //A[4] = 4.0/3.0 * ( 1.0 + 1.0/(2.0*q3) );
            A[4] = ( ( (new Complex(1.0,0.0)).divide(q3.multiply(2.0)) ).add(1.0) ).multiply(4.0/3.0);
            //A[5] = -j * sqrt(PI) / 4.0 * (1.0 + 3.0 / (4.0 * q3));
            A[5] = j.multiply(-Math.sqrt(Math.PI) / 4.0).multiply( ( (new Complex(3.0,0.0)).divide(q3.multiply(4.0))).add(1.0) );
            //A[6] = -8.0/15.0 * ( 1.0 + 1.0/q3 + 7.0 / (32.0 * q6) );
            A[6] = ( (new Complex(1.0,0.0)).divide(q3).add(1.0).add( (new Complex(7.0,0.0)).divide(q6.multiply(32.0)) ) ).multiply(-8.0/15.0);
            //A[7] = j * sqrt(PI) / 6.0 * ( 1.0 + 5.0/(4.0*q3) + 27.0 /( 32.0*q6 ) );
            A[7] = j.multiply(Math.sqrt(Math.PI) / 6.0 ).multiply( ((new Complex(5.0,0.0)).divide(q3.multiply(4.0))).add(1.0).add( (new Complex(27.0,0.0)).divide( q6.multiply(32.0) ) ) );
            //A[8] = 16.0/105.0 * ( 1.0 + 3.0/(2.0*q3) + 27.0/(32.0*q6) );
            A[8] =  (  ( (new Complex(3.0,0.0)).divide(q3.multiply(2.0)) ).add ( (new Complex(27.0,0.0)).divide(q6.multiply(32.0)) ).add(1.0) ).multiply(16.0/105.0);
            //A[9] = -j * sqrt(PI)/24.0 * (1.0 + 7.0/ (4.0*q3) + 5.0/(4.0*q6) + 21.0/(64.0*q9));
            A[9] = j.multiply(-Math.sqrt(Math.PI)/24.0).multiply( ( (new Complex(7.0, 0.0)).divide(q3.multiply(4.0)) ).add( (new Complex(5.0,0.0)).divide(q6.multiply(4.0)) ).add( (new Complex(21.0, 0.0)).divide(q9.multiply(64.0)) ).add(1.0)  );

            double x = d__km / a_e__km * Math.pow(k * a_e__km/2.0, (1.0/3.0));

            fofx = new Complex(0.0, 0.0);

            // [Deminco, Eq 28]
            for (int ii = 0; ii<10; ii++) {
                //fofx = fofx + A[ii] * pow( exp(j * PI/4.0) * q * pow(x,1.0/2.0), ii);
                fofx = fofx.add(  A[ii].multiply( ( ( ( j.multiply(Math.PI/4.0)).exp() ).multiply(q).multiply(Math.pow(x,1.0/2.0)) ).pow(ii) ) );
            }


        }


        // Now find the final normalized field strength from f(x) and the height-gain function for each antenna
        // A height-gain function for an antenna is expressed as two terms of a Taylor series
        // (See DeMinco NTIA Report 99-368 Aug 1999
        // "Medium Frequency Propagation Prediction Techniques and
        // Antenna Modeling for Intelligent Transportation Systems (ITS) Broadcast Applications"
        // Equation 36)

        //double E_gw__mVm = abs(fofx*(1.0 + j * k*h_2__km*Delta)*(1.0 + j * k*h_1__km*Delta));
        double E_gw__mVm = ( fofx .multiply ( (j.multiply(k*h_2__km).multiply(Delta)).add(1.0)  ) .multiply ( (j.multiply(k*h_1__km).multiply(Delta)).add(1.0) ) ).abs();
        double xx = E_gw__mVm;

        return E_gw__mVm;
    }


    int ValidateInput(double h_tx__meter, double h_rx__meter, double f__mhz, double P_tx__watt,
                      double N_s, double d__km, double epsilon, double sigma, int pol)
     /*=============================================================================
     |
     |       Author:  William Kozma Jr
     |                wkozma@ntia.gov
     |                US Dept of Commerce, NTIA/ITS
     |                June 2021 : Geneva Study Group 3 Meetings
     |
     |       Translation to Java: Ivica Stevanovic, OFCOM CH, November 2023
     |
     +-----------------------------------------------------------------------------
     |
     |  Description:  Perform input parameter validation
     |
     |        Input:  h_tx__meter   - Height of the transmitter, in meter
     |                h_rx__meter   - Height of the receiver, in meter
     |                f__mhz        - Frequency, in MHz
     |                P_tx__watt    - Transmitter power, in Watts
     |                N_s           - Surface refractivity, in N-Units
     |                d__km         - Path distance, in km
     |                epsilon       - Relative permittivity
     |                sigma         - Conductivity
     |                pol           - Polarization
     |                                  + 0 : POLARIZATION__HORIZONTAL
     |                                  + 1 : POLARIZATION__VERTICAL
     |
     |      Outputs:  [None]
     |
     |      Returns:  error         - Error code
     |
     *===========================================================================*/

    {
        if (h_tx__meter < 0 || h_tx__meter > 50)
            return Const.ERROR__TX_TERMINAL_HEIGHT;

        if (h_rx__meter < 0 || h_rx__meter > 50)
            return Const.ERROR__RX_TERMINAL_HEIGHT;

        if (f__mhz < 0.01 || f__mhz > 30)
            return Const.ERROR__FREQUENCY;

        if (P_tx__watt <= 0)
            return Const.ERROR__TX_POWER;

        if (N_s < 250 || N_s > 400)
            return Const.ERROR__SURFACE_REFRACTIVITY;

        if (d__km < 0.001 || d__km > 10000)
            return Const.ERROR__PATH_DISTANCE;

        if (epsilon < 1)
            return Const.ERROR__EPSILON;

        if (sigma <= 0)
            return Const.ERROR__SIGMA;

        if (pol != Const.POLARIZATION__HORIZONTAL &&
                pol != Const.POLARIZATION__VERTICAL)
            return Const.ERROR__POLARIZATION;

        return Const.SUCCESS;
    }


    public Result tl_p368(double h_tx__meter, double h_rx__meter, double f__mhz, double P_tx__watt,
             double N_s, double d__km, double epsilon, double sigma, int pol)
    /*=============================================================================
     |
     |       Author:  William Kozma Jr
     |                wkozma@ntia.gov
     |                US Dept of Commerce, NTIA/ITS
     |                June 2021 : Geneva Study Group 3 Meetings
     |
     |       Translation to Java: Ivica Stevanovic, OFCOM CH, November 2023
     |
     +-----------------------------------------------------------------------------
     |
     |  Description:  Compute the LFMF propagation prediction
     |
     |        Input:  h_tx__meter   - Height of the transmitter, in meter
     |                h_rx__meter   - Height of the receiver, in meter
     |                f__mhz        - Frequency, in MHz
     |                P_tx__watt    - Transmitter power, in Watts
     |                N_s           - Surface refractivity, in N-Units
     |                d__km         - Path distance, in km
     |                epsilon       - Relative permittivity
     |                sigma         - Conductivity
     |                pol           - Polarization
     |                                  + 0 : POLARIZATION__HORIZONTAL
     |                                  + 1 : POLARIZATION__VERTICAL
     |
     |      Outputs:  result        - Result structure
     |
     |      Returns:  error         - Error code
     |
     *===========================================================================*/


    {
        Result result = new Result();

        int rtn = ValidateInput(h_tx__meter, h_rx__meter, f__mhz, P_tx__watt, N_s,
                d__km, epsilon, sigma, pol);
        if (rtn != Const.SUCCESS)
            result.err = rtn;


        // Create the complex value j since this was written by electrical engineers
        Complex j = new Complex(0.0, 1.0);

        double f__hz = f__mhz * 1e6;
        double lambda__meter = Const.C / f__hz;                           // wavelength, in meters

        double h_1__km = Math.min(h_tx__meter, h_rx__meter) / 1000;      // lower antenna, in km
        double h_2__km = Math.max(h_tx__meter, h_rx__meter) / 1000;      // higher antenna, in km

        double a_e__km =  Const.a_0__km * 1 / (1 - 0.04665 * Math.exp(0.005577 * N_s));    // effective earth radius, in km

        double theta__rad = d__km / a_e__km;

        double k = 2.0 * Math.PI / (lambda__meter / 1000);               // wave number, in rad/km

        double nu = Math.pow(a_e__km * k / 2.0, Const.THIRD);                  // Intermediate value nu

        // dielectric ground constant. See Eq (17) DeMinco 99-368
        Complex eta = new Complex(epsilon, -sigma / (Const.epsilon_0 * 2 * Math.PI * f__hz));

        // Find the surface impedance, DeMinco 99-368 Eqn (15)
        Complex delta = (eta.subtract(1.0)).sqrt();
        if (pol == Const.POLARIZATION__VERTICAL)
            delta = delta.divide(eta);

        Complex q = j.multiply(-nu).multiply(delta);                         // intermediate value q

        // Determine which smooth earth method is used; SG3 Groundwave Handbook, Eq 15
        double d_test__km = 80 * Math.pow(f__mhz, -Const.THIRD);

        double E_gw;
        if (d__km < d_test__km)
        {
            E_gw = FlatEarthCurveCorrection(delta, q, h_1__km, h_2__km, d__km, k, a_e__km);
            result.method = Const.METHOD__FLAT_EARTH_CURVE;

        }
        else
        {
            E_gw = ResidueSeries(d__km, k, h_1__km, h_2__km, nu, theta__rad, q);

            result.method = Const.METHOD__RESIDUE_SERIES;
        }

        // Antenna gains
        double G_tx__dbi = 4.77;
        double G_rx__dbi = 4.77;

        double G_tx = Math.pow(10, G_tx__dbi / 10);

        // Un-normalize the electric field strength
        double E_0 = Math.sqrt(Const.ETA * (P_tx__watt * G_tx) / (4.0 * Math.PI)) / d__km;  // V/km or mV/m
        E_gw = E_gw * E_0;

        // Calculate the basic transmission loss using (derived using Friis Transmission Equation with Electric Field Strength)
        //      Pt     Gt * Pt * ETA * 4*PI * f^2
        // L = ---- = ---------------------------  and convert to dB
        //      Pr            E^2 * c^2
        // with all values entered using base units: Watts, Hz, and V/m
        // basic transmission loss is not a function of power/gain, but since electric field strength E_gw is a function of (Gt * Pt),
        //    and Lbtl is a function of 1/E_gw, we add in (Gt * Pt) to remove its effects
        result.A_btl__db = 10 * Math.log10(P_tx__watt * G_tx)
                + 10 * Math.log10(Const.ETA * 4 * Math.PI)
                + 20 * Math.log10(f__hz)
                - 20 * Math.log10(E_gw / 1000)
                - 20 * Math.log10(Const.C);

        // the 60 constant comes from converting field strength from mV/m to dB(uV/m) thus 20*log10(1e3)
        result.E_dBuVm = 60 + 20 * Math.log10(E_gw);

        // Note power is a function of frequency.  42.8 comes from MHz to hz, power in dBm, and the remainder from
        // the collection of constants in the derivation of the below equation.
        result.P_rx__dbm = result.E_dBuVm + G_rx__dbi - 20.0*Math.log10(f__hz) + 42.8;

        return result;
    }

}

__kernel void isEven(__global int* input, __global int* output) {
     int globalId = get_global_id(0);
     output[globalId] = input[globalId] % 2 == 0;
}

__kernel void isEven(__global int* input, __global int* output) {
     int globalId = get_global_id(0);
     output[globalId] = input[globalId] % 2 == 0;
}

__kernel void isEven(__global int* input, __global int* output) {
     int globalId = get_global_id(0);
     output[globalId] = input[globalId] % 2 == 0;
}

__kernel void scatter(__global int* input, __global int* filterArray, __global int* indexArray, __global int* output) {
    int globalId = get_global_id(0);
    if(filterArray[globalId] == 1) {
        int targetIndex = indexArray[globalId];
        output[targetIndex] = input[globalId];
    }
}
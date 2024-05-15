import shutil
import os
import subprocess

def copy_files_and_run_command(src_dir, dest_dir, command):

    copied_files = []
    # Copy files from source directory to destination directory
    for filename in os.listdir(src_dir):
        src_path = os.path.join(src_dir, filename)
        dest_path = os.path.join(dest_dir, filename)
        shutil.copy(src_path, dest_path)
        copied_files.append(dest_path)

    # Change directory to the destination directory
    os.chdir(dest_dir+"/../../")

    # Run the command in the destination directory
    subprocess.run(command, shell=True)

    # Clean the directory from copied files
    for file_path in copied_files:
        os.remove(file_path)

if __name__ == "__main__":
    # Replace these paths and command with your actual paths and command
    source_directory = "test_conf"
    destination_directory = "/usr/local/apache2/conf/testing"
    command_to_run = "sudo ./bin/httpd -t -D DUMP_CONFIG > /home/bertrandvano/Documents/CWAF/conf/dump_conf_test.txt"

    copy_files_and_run_command(source_directory, destination_directory, command_to_run)




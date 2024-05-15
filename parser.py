import re
from prototype import Directive

def parse_compiled_config(file_path):
    directives = []

    # Regular expressions for extracting relevant information
    server_name_pattern = re.compile(r'[ \t]*SetEnv (\S+)')
    virtual_host_pattern = re.compile(r'<VirtualHost\s+(.*?)>')
    virtual_host_end_pattern = re.compile(r'</VirtualHost>')
    location_pattern = re.compile(r'[ \t]*<Location\s+(.*?)>')
    location_end_pattern = re.compile(r'[ \t]*</Location>')
    if_pattern = re.compile(r'[ \t]*<If\s+')
    if_pattern_end = re.compile(r'[ \t]*</If>')
    file_flag = re.compile(r'# In file:')
    line_numbers_regex = re.compile(r'used on line (\d+)')
    macro_names_regex = re.compile(r'macro \'(\w+)\'')
    instruction_number_pattern = re.compile(r'#\s+(\d+):')

    with open(file_path, 'r') as file:
        lines = file.readlines()

    current_virtualhost = None
    current_location = None
    current_if_level = 0
    current_ordering_number = 0
    current_macro_stack = None
    current_instruction_number = None

    for line in lines:

        # Extract VirtualHost
        match_virtual_host = virtual_host_pattern.match(line)
        if match_virtual_host:
            current_virtualhost = "VHOST"#match_virtual_host.group(1)
            current_location = None
            current_if_level = 0
            continue
            
        # End VirtualHost
        match_virtual_host_end = virtual_host_end_pattern.match(line)
        if match_virtual_host_end:
            current_virtualhost = ""#match_virtual_host.group(1)
            continue

        # Extract Location
        match_location = location_pattern.match(line)
        if match_location:
            current_location = "LOC"#match_location.group(1)
            continue
            
        # End Location
        match_location_end = location_end_pattern.match(line)
        if match_location_end:
            current_location = ""

        # Determine if level
        if if_pattern.match(line):
            current_if_level += 1
        if if_pattern_end.match(line):
            current_if_level -= 1

        if file_flag.match(line):
            current_instruction_number = line_numbers_regex.findall(line)
            current_macro_stack = macro_names_regex.findall(line) 
            
        # Extract instruction number
        match_instruction_number = instruction_number_pattern.match(line)
        if match_instruction_number and len(current_macro_stack) == 0:
            current_instruction_number = int(match_instruction_number.group(1))    
        
        # Extract ServerName
        match_server_name = server_name_pattern.match(line)
        if match_server_name:
            server_name = server_name_pattern.findall(line)[0]
            current_ordering_number = current_ordering_number+1
            print(current_macro_stack)
            print(current_instruction_number)
            new_directive = Directive(current_location,current_virtualhost, current_if_level, current_macro_stack, current_instruction_number,current_ordering_number,server_name)
            directives.append(new_directive)

    print(directives)
    return directives

file_path = "dump_conf_order3.txt"  # Replace with the path to your config file
directives = parse_compiled_config(file_path)
sorted_directives = sorted(directives)
for directive in sorted_directives:
    print(directive)


